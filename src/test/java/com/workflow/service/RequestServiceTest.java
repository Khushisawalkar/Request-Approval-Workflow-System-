package com.workflow.service;

import com.workflow.dto.CreateRequestDto;
import com.workflow.dto.RequestResponseDto;
import com.workflow.dto.UpdateRequestDto;
import com.workflow.dto.WorkflowDecisionDto;
import com.workflow.exception.InvalidStatusTransitionException;
import com.workflow.exception.UnauthorizedActionException;
import com.workflow.model.*;
import com.workflow.repository.RequestRepository;
import com.workflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private UserService userService;

    @InjectMocks
    private RequestService requestService;

    private User employee;
    private User manager;
    private User otherManager;
    private Request pendingRequest;

    @BeforeEach
    void setUp() {
        manager = User.builder()
                .id(2L)
                .username("manager1")
                .fullName("Alex Manager")
                .roles(Set.of(RoleName.ROLE_MANAGER))
                .build();

        otherManager = User.builder()
                .id(3L)
                .username("manager2")
                .fullName("David Manager")
                .roles(Set.of(RoleName.ROLE_MANAGER))
                .build();

        employee = User.builder()
                .id(1L)
                .username("employee1")
                .fullName("Sarah Employee")
                .manager(manager)
                .roles(Set.of(RoleName.ROLE_EMPLOYEE))
                .build();

        pendingRequest = Request.builder()
                .id(100L)
                .title("New Laptop Request")
                .description("Dell XPS 15")
                .category(RequestCategory.EQUIPMENT)
                .priority(RequestPriority.HIGH)
                .status(RequestStatus.PENDING)
                .submittedBy(employee)
                .assignedManager(manager)
                .build();
    }

    @Test
    @DisplayName("Should successfully create a request in PENDING status")
    void testCreateRequest_Success() {
        CreateRequestDto dto = CreateRequestDto.builder()
                .title("New Laptop Request")
                .description("Dell XPS 15")
                .category(RequestCategory.EQUIPMENT)
                .priority(RequestPriority.HIGH)
                .build();

        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> {
            Request r = invocation.getArgument(0);
            r.setId(100L);
            return r;
        });

        RequestResponseDto response = requestService.createRequest(dto, "employee1");

        assertNotNull(response);
        assertEquals("New Laptop Request", response.getTitle());
        assertEquals(RequestStatus.PENDING, response.getStatus());
        verify(requestRepository, times(1)).save(any(Request.class));
        verify(auditLogService, times(1)).record(anyLong(), eq("SUBMITTED"), eq("employee1"), any(), any(), eq(RequestStatus.PENDING), anyString());
    }

    @Test
    @DisplayName("Should approve request when authorized assigned manager decides")
    void testApproveRequest_Success() {
        WorkflowDecisionDto decision = new WorkflowDecisionDto("Approved as budgeted.");
        when(requestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequestResponseDto response = requestService.approveRequest(100L, decision, "manager1");

        assertNotNull(response);
        assertEquals(RequestStatus.APPROVED, response.getStatus());
        assertEquals("Approved as budgeted.", response.getDecisionComment());
        verify(auditLogService, times(1)).record(100L, "APPROVED", "manager1", "ROLE_MANAGER", RequestStatus.PENDING, RequestStatus.APPROVED, "Approved as budgeted.");
    }

    @Test
    @DisplayName("Should reject request when mandatory rejection reason is provided")
    void testRejectRequest_Success() {
        WorkflowDecisionDto decision = new WorkflowDecisionDto("Budget exceeded for this quarter.");
        when(requestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequestResponseDto response = requestService.rejectRequest(100L, decision, "manager1");

        assertNotNull(response);
        assertEquals(RequestStatus.REJECTED, response.getStatus());
        assertEquals("Budget exceeded for this quarter.", response.getDecisionComment());
        verify(auditLogService, times(1)).record(100L, "REJECTED", "manager1", "ROLE_MANAGER", RequestStatus.PENDING, RequestStatus.REJECTED, "Budget exceeded for this quarter.");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when rejecting without a reason")
    void testRejectRequest_MissingComment_ThrowsException() {
        WorkflowDecisionDto decision = new WorkflowDecisionDto("");

        assertThrows(IllegalArgumentException.class, () ->
                requestService.rejectRequest(100L, decision, "manager1")
        );

        verify(requestRepository, never()).save(any(Request.class));
    }

    @Test
    @DisplayName("Should prevent self-approval if an employee tries to approve their own request")
    void testApproveRequest_SelfApproval_ThrowsUnauthorized() {
        pendingRequest.setAssignedManager(employee); // simulates edge case
        when(requestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("employee1")).thenReturn(Optional.of(employee));

        assertThrows(UnauthorizedActionException.class, () ->
                requestService.approveRequest(100L, new WorkflowDecisionDto("self approved"), "employee1")
        );
    }

    @Test
    @DisplayName("Should prevent unassigned manager from approving request")
    void testApproveRequest_UnauthorizedManager_ThrowsUnauthorized() {
        when(requestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("manager2")).thenReturn(Optional.of(otherManager));

        assertThrows(UnauthorizedActionException.class, () ->
                requestService.approveRequest(100L, new WorkflowDecisionDto("wrong manager"), "manager2")
        );
    }

    @Test
    @DisplayName("Should throw InvalidStatusTransitionException when acting on already APPROVED request")
    void testApproveRequest_AlreadyApproved_ThrowsException() {
        pendingRequest.setStatus(RequestStatus.APPROVED);
        when(requestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findByUsername("manager1")).thenReturn(Optional.of(manager));

        assertThrows(InvalidStatusTransitionException.class, () ->
                requestService.approveRequest(100L, new WorkflowDecisionDto("duplicate"), "manager1")
        );
    }

    @Test
    @DisplayName("Should cancel request when requested by the original submitter")
    void testCancelRequest_Success() {
        when(requestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));
        when(requestRepository.save(any(Request.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RequestResponseDto response = requestService.cancelRequest(100L, "employee1");

        assertNotNull(response);
        assertEquals(RequestStatus.CANCELLED, response.getStatus());
        verify(auditLogService, times(1)).record(100L, "CANCELLED", "employee1", "ROLE_EMPLOYEE", RequestStatus.PENDING, RequestStatus.CANCELLED, "Cancelled by requester");
    }

    @Test
    @DisplayName("Should prevent another user from cancelling someone else's request")
    void testCancelRequest_UnauthorizedUser_ThrowsException() {
        when(requestRepository.findById(100L)).thenReturn(Optional.of(pendingRequest));

        assertThrows(UnauthorizedActionException.class, () ->
                requestService.cancelRequest(100L, "manager1")
        );
    }
}

