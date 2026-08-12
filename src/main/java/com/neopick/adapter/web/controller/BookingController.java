package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.booking.BookingResponse;
import com.neopick.adapter.web.dto.booking.CancelBookingRequest;
import com.neopick.adapter.web.dto.booking.SubmitBookingRequest;
import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.common.PageResponse;
import com.neopick.application.booking.*;
import com.neopick.domain.booking.Booking;
import com.neopick.infrastructure.ratelimit.RateLimit;
import com.neopick.shared.Constants;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
@Tag(name = "Bookings", description = "Booking lifecycle management: create, confirm, reject, cancel, and complete")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final SubmitBookingUseCase submitBookingUseCase;
    private final ManageBookingUseCase manageBookingUseCase;
    private final GetStudentBookingsUseCase getStudentBookingsUseCase;
    private final GetTeacherBookingsUseCase getTeacherBookingsUseCase;

    public BookingController(SubmitBookingUseCase submitBookingUseCase,
                             ManageBookingUseCase manageBookingUseCase,
                             GetStudentBookingsUseCase getStudentBookingsUseCase,
                             GetTeacherBookingsUseCase getTeacherBookingsUseCase) {
        this.submitBookingUseCase = submitBookingUseCase;
        this.manageBookingUseCase = manageBookingUseCase;
        this.getStudentBookingsUseCase = getStudentBookingsUseCase;
        this.getTeacherBookingsUseCase = getTeacherBookingsUseCase;
    }

    @PostMapping
    @RateLimit(limit = 10, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.bookings.submit", description = "Booking submission")
    @Operation(summary = "Submit a booking request", description = "Creates a new booking request for a student to book a lesson with a teacher. The booking starts in PENDING state.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking request submitted successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request — missing required fields or invalid data", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Teacher not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Time slot already booked", content = @Content)
    })
    public ApiResponse<BookingResponse> submit(@Valid @RequestBody SubmitBookingRequest request) {
        Booking booking = submitBookingUseCase.execute(new SubmitBookingUseCase.SubmitBookingCommand(
                request.teacherId(), request.scheduledStart(), request.durationMinutes(),
                request.price(), request.addressLabel(), request.addressDetail(),
                request.latitude(), request.longitude(), request.note()));
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @GetMapping
    @RateLimit(limit = 30, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.bookings.list_student", description = "List student bookings")
    @Operation(summary = "List student bookings", description = "Returns the authenticated student's bookings, optionally filtered by status. Results are paginated.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Student bookings returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<PageResponse<BookingResponse>> listStudent(
            @Parameter(description = "Filter by booking status (PENDING, CONFIRMED, COMPLETED, CANCELLED)") @RequestParam(required = false) String status,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, Constants.MAX_PAGE_SIZE);
        var result = getStudentBookingsUseCase.execute(status, page, safeSize);
        List<BookingResponse> items = result.bookings().stream().map(BookingResponse::from).toList();
        return ApiResponse.success(PageResponse.of(items, page, safeSize, result.total()));
    }

    @GetMapping("/{id}")
    @Timed(value = "neopick.bookings.get_by_id", description = "Get booking detail")
    @Operation(summary = "Get booking detail", description = "Returns full details of a specific booking by its ID.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking detail returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found", content = @Content)
    })
    public ApiResponse<BookingResponse> getById(
            @Parameter(description = "Booking ID (UUID format)") @PathVariable String id) {
        Booking booking = manageBookingUseCase.getDetail(id);
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @PutMapping("/{id}/confirm")
    @RateLimit(limit = 20, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.bookings.confirm")
    @Operation(summary = "Confirm a booking", description = "Teacher confirms a pending booking request. Status changes from PENDING to CONFIRMED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking confirmed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Booking is not in PENDING state", content = @Content)
    })
    public ApiResponse<BookingResponse> confirm(
            @Parameter(description = "Booking ID (UUID format)") @PathVariable String id) {
        Booking booking = manageBookingUseCase.confirm(id);
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @PutMapping("/{id}/reject")
    @RateLimit(limit = 20, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.bookings.reject", description = "Reject booking")
    @Operation(summary = "Reject a booking", description = "Teacher rejects a pending booking request with a reason. Status changes from PENDING to REJECTED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking rejected successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Rejection reason is required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Booking is not in PENDING state", content = @Content)
    })
    public ApiResponse<BookingResponse> reject(
            @Parameter(description = "Booking ID (UUID format)") @PathVariable String id,
            @Valid @RequestBody CancelBookingRequest request) {
        Booking booking = manageBookingUseCase.reject(id, request.reason());
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @PutMapping("/{id}/cancel")
    @RateLimit(limit = 20, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.bookings.cancel", description = "Cancel booking")
    @Operation(summary = "Cancel a booking", description = "Student or teacher cancels a confirmed booking with a reason. Status changes to CANCELLED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Cancellation reason is required", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Booking cannot be cancelled in current state", content = @Content)
    })
    public ApiResponse<BookingResponse> cancel(
            @Parameter(description = "Booking ID (UUID format)") @PathVariable String id,
            @Valid @RequestBody CancelBookingRequest request) {
        Booking booking = manageBookingUseCase.cancel(id, request.reason());
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @PutMapping("/{id}/complete")
    @RateLimit(limit = 20, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.bookings.complete")
    @Operation(summary = "Complete a booking", description = "Marks a confirmed booking as completed after the lesson has finished. Status changes to COMPLETED.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Booking completed successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Booking cannot be completed in current state", content = @Content)
    })
    public ApiResponse<BookingResponse> complete(
            @Parameter(description = "Booking ID (UUID format)") @PathVariable String id) {
        Booking booking = manageBookingUseCase.complete(id);
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @GetMapping("/teacher")
    @RateLimit(limit = 30, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.bookings.list_teacher", description = "List teacher bookings")
    @Operation(summary = "List teacher bookings", description = "Returns a teacher's bookings, optionally filtered by status. Results are paginated.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Teacher bookings returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<PageResponse<BookingResponse>> listTeacher(
            @Parameter(description = "Teacher ID") @RequestParam Long teacherId,
            @Parameter(description = "Filter by booking status") @RequestParam(required = false) String status,
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, Constants.MAX_PAGE_SIZE);
        var result = getTeacherBookingsUseCase.execute(teacherId, status, page, safeSize);
        List<BookingResponse> items = result.bookings().stream().map(BookingResponse::from).toList();
        return ApiResponse.success(PageResponse.of(items, page, safeSize, result.total()));
    }
}
