package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.booking.BookingResponse;
import com.neopick.adapter.web.dto.booking.CancelBookingRequest;
import com.neopick.adapter.web.dto.booking.SubmitBookingRequest;
import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.common.PageResponse;
import com.neopick.application.booking.*;
import com.neopick.domain.booking.Booking;
import com.neopick.shared.Constants;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/bookings")
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
    public ApiResponse<BookingResponse> submit(@Valid @RequestBody SubmitBookingRequest request) {
        Booking booking = submitBookingUseCase.execute(new SubmitBookingUseCase.SubmitBookingCommand(
                request.teacherId(), request.scheduledStart(), request.durationMinutes(),
                request.price(), request.addressLabel(), request.addressDetail(),
                request.latitude(), request.longitude(), request.note()));
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @GetMapping
    public ApiResponse<PageResponse<BookingResponse>> listStudent(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, Constants.MAX_PAGE_SIZE);
        var result = getStudentBookingsUseCase.execute(status, page, safeSize);
        List<BookingResponse> items = result.bookings().stream().map(BookingResponse::from).toList();
        return ApiResponse.success(PageResponse.of(items, page, safeSize, result.total()));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> getById(@PathVariable String id) {
        Booking booking = manageBookingUseCase.getDetail(id);
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<BookingResponse> confirm(@PathVariable String id) {
        Booking booking = manageBookingUseCase.confirm(id);
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<BookingResponse> reject(@PathVariable String id,
                                                @RequestBody CancelBookingRequest request) {
        Booking booking = manageBookingUseCase.reject(id, request.reason());
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<BookingResponse> cancel(@PathVariable String id,
                                                @RequestBody CancelBookingRequest request) {
        Booking booking = manageBookingUseCase.cancel(id, request.reason());
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @PutMapping("/{id}/complete")
    public ApiResponse<BookingResponse> complete(@PathVariable String id) {
        Booking booking = manageBookingUseCase.complete(id);
        return ApiResponse.success(BookingResponse.from(booking));
    }

    @GetMapping("/teacher")
    public ApiResponse<PageResponse<BookingResponse>> listTeacher(
            @RequestParam Long teacherId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(size, Constants.MAX_PAGE_SIZE);
        var result = getTeacherBookingsUseCase.execute(teacherId, status, page, safeSize);
        List<BookingResponse> items = result.bookings().stream().map(BookingResponse::from).toList();
        return ApiResponse.success(PageResponse.of(items, page, safeSize, result.total()));
    }
}
