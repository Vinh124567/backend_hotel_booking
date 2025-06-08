package com.example.demo.service.booking;
import com.example.demo.dto.booking.BookingRequest;
import com.example.demo.dto.booking.BookingResponse;
import com.example.demo.dto.booking.BookingStatsResponse;
import com.example.demo.dto.hotel_image.HotelImageResponse;
import com.example.demo.entity.Booking;
import com.example.demo.entity.Hotel;
import com.example.demo.entity.RoomType;
import com.example.demo.entity.User;
import com.example.demo.repository.BookingRepository;
import com.example.demo.repository.RoomTypeRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.user.UserService;
import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class BookingServiceImpl implements BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingServiceImpl.class);

    private final BookingRepository bookingRepository;
    private final UserService userService; // ✅ Inject UserService

    private final RoomTypeRepository roomTypeRepository;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;

    @Transactional
    @Override
    public BookingResponse createBooking(BookingRequest request) {
        log.info("🏨 Creating booking for roomTypeId: {}, checkIn: {}, checkOut: {}",
                request.getRoomTypeId(), request.getCheckInDate(), request.getCheckOutDate());
        User currentUser = userService.getCurrentUser(); // ✅ Use getCurrentUser()

        // 1. Validate request
        validateBookingRequest(request);

        // 2. Check room type exists
        RoomType roomType = roomTypeRepository.findById(request.getRoomTypeId())
                .orElseThrow(() -> new RuntimeException("Loại phòng không tồn tại với ID: " + request.getRoomTypeId()));

        // 3. Check availability
        if (!isRoomTypeAvailable(request.getRoomTypeId(), request.getCheckInDate(), request.getCheckOutDate())) {
            throw new RuntimeException("Loại phòng không khả dụng trong thời gian đã chọn");
        }
        // 5. Create booking entity
        Booking booking = new Booking();
        booking.setUser(currentUser);
        booking.setRoomType(roomType);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setTotalPrice(BigDecimal.valueOf(request.getTotalPrice()));
        booking.setSpecialRequests(request.getSpecialRequests());
        booking.setStatus("Chờ xác nhận"); // Default status from entity
        // bookingDate will be set by @PrePersist

        // 6. Save booking
        booking = bookingRepository.save(booking);

        log.info("✅ Booking created successfully with ID: {}", booking.getId());

        return mapToBookingResponse(booking);
    }

    @Override
    public BookingResponse cancelBooking(Long bookingId) {
        // 1. Tìm booking theo ID
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));
        User currentUser = userService.getCurrentUser(); // ✅ Use getCurrentUser()

        // 2. Kiểm tra quyền sở hữu
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền hủy đặt phòng này");
        }

        // 3. Kiểm tra trạng thái có thể hủy
        validateCancellation(booking);

        // 4. Cập nhật trạng thái
        booking.setStatus("Đã hủy");

        // 5. Lưu và trả về
        Booking savedBooking = bookingRepository.save(booking);
        BookingResponse response = convertToBookingResponse(savedBooking);
        return response;
    }

    @Override
    public List<BookingResponse> getPendingBookings() {
        log.info("📋 Getting all pending confirmation bookings");

        // Sử dụng method có sẵn trong BookingRepository
        List<Booking> pendingBookings = bookingRepository.findByStatusOrderByBookingDateDesc("Chờ xác nhận");

        return pendingBookings.stream()
                .map(this::convertToBookingResponse)
                .toList();
    }

    @Override
    public List<BookingResponse> getCurrentUserBookings() {
        User currentUser = userService.getCurrentUser(); // ✅ Use getCurrentUser()
        log.info("📋 Getting bookings for user: {}", currentUser.getUsername());

        List<Booking> bookings = bookingRepository.findByUserIdOrderByBookingDateDesc(currentUser.getId());
        return bookings.stream()
                .map(this::mapToBookingResponse)
                .toList();
    }


    @Override
    public boolean isRoomTypeAvailable(Long roomTypeId, LocalDate checkInDate, LocalDate checkOutDate) {
        log.info("🔍 Checking availability for roomType: {} from {} to {}",
                roomTypeId, checkInDate, checkOutDate);

        try {
            // Check if roomType exists first
            RoomType roomType = roomTypeRepository.findById(roomTypeId)
                    .orElseThrow(() -> new RuntimeException("Loại phòng không tồn tại"));

            log.info("✅ RoomType found: {}", roomType.getTypeName());

            // Count overlapping bookings
            long bookedRooms = bookingRepository.countOverlappingBookings(roomTypeId, checkInDate, checkOutDate);
            log.info("📊 Booked rooms: {}", bookedRooms);

            // SIMPLE FIX: Assume each room type has 5 rooms available
            long totalRooms = 5; // Fixed number for development
            boolean isAvailable = bookedRooms < totalRooms;

            log.info("📊 Result - Total: {}, Booked: {}, Available: {}",
                    totalRooms, bookedRooms, isAvailable);

            return isAvailable;

        } catch (Exception e) {
            log.error("❌ Availability check error: ", e);
            // For development: return true to allow booking
            log.warn("⚠️ DEVELOPMENT MODE: Returning true for testing");
            return true;
        }
    }

    // ========== Helper Methods ==========

    private void validateBookingRequest(BookingRequest request) {
        if (request.getCheckInDate().isAfter(request.getCheckOutDate())) {
            throw new RuntimeException("Ngày check-in phải trước ngày check-out");
        }

        if (request.getCheckInDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Ngày check-in không thể là quá khứ");
        }

        if (request.getNumberOfGuests() <= 0) {
            throw new RuntimeException("Số lượng khách phải lớn hơn 0");
        }

        if (request.getTotalPrice() <= 0) {
            throw new RuntimeException("Tổng giá phải lớn hơn 0");
        }
    }

    private boolean canCancelBooking(Booking booking) {
        // Cannot cancel if already cancelled
        if ("Đã hủy".equals(booking.getStatus())) {
            return false;
        }

        // Cannot cancel if already paid or checked in
        if ("Đã thanh toán".equals(booking.getStatus()) ||
                "Đã nhận phòng".equals(booking.getStatus()) ||
                "Đã trả phòng".equals(booking.getStatus())) {
            return false;
        }

        // Cannot cancel within 24 hours of check-in
        return booking.getCheckInDate().isAfter(LocalDate.now().plusDays(1));
    }



    @Override
    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        return convertToBookingResponse(booking);
    }



    // ========== PERMISSION CALCULATION METHODS ==========

    private Boolean calculateCanCancel(Booking booking) {
        String status = booking.getStatus();

        // Không thể hủy nếu đã hủy, hoàn thành, hoặc đã nhận phòng
        if ("Đã hủy".equals(status) ||
                "Hoàn thành".equals(status) ||
                "Đã nhận phòng".equals(status) ||
                "Checked In".equals(status)) {
            return false;
        }

        // Kiểm tra thời hạn 24h
        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);

        return daysUntilCheckIn >= 1;
    }

    private Boolean calculateCanModify(Booking booking) {
        String status = booking.getStatus();

        // Không thể sửa nếu đã hủy, hoàn thành, hoặc đã nhận phòng
        if ("Đã hủy".equals(status) ||
                "Hoàn thành".equals(status) ||
                "Đã nhận phòng".equals(status) ||
                "Checked In".equals(status)) {
            return false;
        }

        // Có thể sửa trong vòng 48h trước check-in
        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);

        return daysUntilCheckIn >= 2;
    }

    private Boolean calculateCanCheckIn(Booking booking) {
        String status = booking.getStatus();

        // ✅ Cho phép check-in từ "Chờ xác nhận" hoặc "Đã xác nhận"
        if (!"Chờ xác nhận".equals(status) && !"Đã xác nhận".equals(status)) {
            return false;
        }

        // Chỉ có thể check-in trong ngày check-in hoặc sau ngày check-in
        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();

        return now.equals(checkInDate) || now.isAfter(checkInDate);
    }

    private Boolean calculateCanCheckOut(Booking booking) {
        String status = booking.getStatus();

        // Chỉ có thể check-out nếu đã check-in
        if (!"Đã nhận phòng".equals(status) && !"Checked In".equals(status)) {
            return false;
        }

        // Có thể check-out từ ngày check-in đến ngày check-out
        LocalDate checkOutDate = booking.getCheckOutDate();
        LocalDate now = LocalDate.now();

        return now.isBefore(checkOutDate) || now.equals(checkOutDate);
    }

    // ========== ADDITIONAL BOOKING METHODS ==========

    @Override
    public BookingResponse checkInBooking(Long bookingId) {
        User currentUser = userService.getCurrentUser(); // ✅ Option 2

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        // Kiểm tra quyền sở hữu
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền check-in đặt phòng này");
        }

        // Validate check-in
        validateCheckIn(booking);

        // Cập nhật status
        booking.setStatus("Đã nhận phòng");

        Booking savedBooking = bookingRepository.save(booking);
        return convertToBookingResponse(savedBooking);
    }

    @Override
    public BookingResponse checkOutBooking(Long bookingId) {
        User currentUser = userService.getCurrentUser(); // ✅ Option 2

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        // Kiểm tra quyền sở hữu
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền check-out đặt phòng này");
        }

        // Validate check-out
        validateCheckOut(booking);

        // Cập nhật status
        booking.setStatus("Hoàn thành");

        Booking savedBooking = bookingRepository.save(booking);
        return convertToBookingResponse(savedBooking);
    }

    @Override
    public BookingResponse updateBooking(Long bookingId, BookingRequest request) {
        User currentUser = userService.getCurrentUser(); // ✅ Option 2

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        // Kiểm tra quyền sở hữu
        if (!booking.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("Bạn không có quyền cập nhật đặt phòng này");
        }

        // Validate modification
        validateModification(booking);

        // Update fields
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setTotalPrice(java.math.BigDecimal.valueOf(request.getTotalPrice()));
        booking.setSpecialRequests(request.getSpecialRequests());

        Booking savedBooking = bookingRepository.save(booking);
        return convertToBookingResponse(savedBooking);
    }

    @Override
    public BookingStatsResponse getUserBookingStats() {
        User currentUser = userService.getCurrentUser(); // ✅ Option 2

        List<Booking> userBookings = bookingRepository.findByUserId(currentUser.getId());

        Long totalBookings = (long) userBookings.size();
        Long activeBookings = userBookings.stream()
                .filter(b -> "Đã xác nhận".equals(b.getStatus()) || "Đã nhận phòng".equals(b.getStatus()))
                .count();
        Long completedBookings = userBookings.stream()
                .filter(b -> "Hoàn thành".equals(b.getStatus()))
                .count();
        Long cancelledBookings = userBookings.stream()
                .filter(b -> "Đã hủy".equals(b.getStatus()))
                .count();

        Double totalSpent = userBookings.stream()
                .filter(b -> !"Đã hủy".equals(b.getStatus()))
                .mapToDouble(b -> b.getTotalPrice().doubleValue())
                .sum();

        Long favoriteHotels = (long) currentUser.getFavoriteHotels().size();

        return BookingStatsResponse.builder()
                .totalBookings(totalBookings)
                .activeBookings(activeBookings)
                .completedBookings(completedBookings)
                .cancelledBookings(cancelledBookings)
                .totalSpent(totalSpent)
                .favoriteHotels(favoriteHotels)
                .build();
    }

    @Override
    public BookingResponse confirmBooking(Long bookingId) {
        log.info("🔔 Confirming booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt phòng với ID: " + bookingId));

        // Validate current status
        if (!"Chờ xác nhận".equals(booking.getStatus())) {
            throw new RuntimeException("Chỉ có thể xác nhận đặt phòng ở trạng thái 'Chờ xác nhận'");
        }

        // Update status
        booking.setStatus("Đã xác nhận");

        Booking savedBooking = bookingRepository.save(booking);
        log.info("✅ Booking confirmed successfully: {}", bookingId);

        return convertToBookingResponse(savedBooking);
    }

    private void validateCancellation(Booking booking) {
        String status = booking.getStatus();

        // Đã bị hủy
        if ("Đã hủy".equals(status)) {
            throw new RuntimeException("Đặt phòng đã bị hủy trước đó");
        }

        // Đã hoàn thành
        if ("Hoàn thành".equals(status)) {
            throw new RuntimeException("Không thể hủy - Đặt phòng đã hoàn thành");
        }

        // Đã check-in
        if ("Đã nhận phòng".equals(status)) {
            throw new RuntimeException("Không thể hủy - Đã nhận phòng");
        }

        // Kiểm tra thời hạn 24h
        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);

        if (daysUntilCheckIn < 1) {
            throw new RuntimeException("Không thể hủy - Quá thời hạn 24 giờ trước check-in");
        }
    }

    @Scheduled(fixedRate = 60000) // Every 5 minutes
    @Transactional
    public void cleanupExpiredPendingBookings() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(1);
        List<Booking> expiredBookings = bookingRepository.findByStatusAndBookingDateBefore("Chờ xác nhận", cutoffTime);

        if (!expiredBookings.isEmpty()) {
            expiredBookings.forEach(booking -> booking.setStatus("Đã hủy"));
            bookingRepository.saveAll(expiredBookings);
            log.info("✅ Auto-cancelled {} expired bookings", expiredBookings.size());
        }
    }

    private void validateCheckIn(Booking booking) {
        String status = booking.getStatus();

        // ✅ OPTION 1: Cho phép check-in từ cả "Chờ xác nhận" và "Đã xác nhận"
        if (!"Chờ xác nhận".equals(status) && !"Đã xác nhận".equals(status)) {
            throw new RuntimeException("Chỉ có thể check-in đặt phòng ở trạng thái 'Chờ xác nhận' hoặc 'Đã xác nhận'");
        }


        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();

        if (now.isBefore(checkInDate)) {
            throw new RuntimeException("Chưa đến ngày check-in");
        }

        // Có thể check-in trong vòng 1 ngày sau check-in date
        if (ChronoUnit.DAYS.between(checkInDate, now) > 1) {
            throw new RuntimeException("Đã quá thời gian check-in");
        }
    }


    private void validateCheckOut(Booking booking) {
        String status = booking.getStatus();

        if (!"Đã nhận phòng".equals(status)) {
            throw new RuntimeException("Chỉ có thể check-out sau khi đã check-in");
        }

        LocalDate checkOutDate = booking.getCheckOutDate();
        LocalDate now = LocalDate.now();

        if (now.isAfter(checkOutDate.plusDays(1))) {
            throw new RuntimeException("Đã quá thời gian check-out");
        }
    }

    private void validateModification(Booking booking) {
        String status = booking.getStatus();

        if ("Đã hủy".equals(status) || "Hoàn thành".equals(status) || "Đã nhận phòng".equals(status)) {
            throw new RuntimeException("Không thể sửa đổi đặt phòng ở trạng thái hiện tại");
        }

        LocalDate checkInDate = booking.getCheckInDate();
        LocalDate now = LocalDate.now();
        long daysUntilCheckIn = ChronoUnit.DAYS.between(now, checkInDate);

        if (daysUntilCheckIn < 2) {
            throw new RuntimeException("Không thể sửa đổi - Phải trước 48 giờ so với check-in");
        }
    }


    private BookingResponse convertToBookingResponse(Booking booking) {
        BookingResponse response = modelMapper.map(booking, BookingResponse.class);

        // ========== BASIC BOOKING FIELDS ==========
        response.setUserId(booking.getUser().getId());
        response.setRoomTypeId(booking.getRoomType().getId());
        response.setRoomTypeName(booking.getRoomType().getTypeName());
        response.setUserName(booking.getUser().getFullName());
        response.setUserEmail(booking.getUser().getEmail());

        if (booking.getAssignedRoom() != null) {
            response.setAssignedRoomId(booking.getAssignedRoom().getId());
            response.setRoomNumber(booking.getAssignedRoom().getRoomNumber());
        }

        // Convert BigDecimal to Double
        if (booking.getTotalPrice() != null) {
            response.setTotalPrice(booking.getTotalPrice().doubleValue());
        }

        // ========== ✅ NEW: HOTEL INFORMATION FROM ROOMTYPE -> HOTEL ==========
        RoomType roomType = booking.getRoomType();
        Hotel hotel = roomType.getHotel();

        if (hotel != null) {
            // Basic hotel info
            response.setHotelId(hotel.getId());
            response.setHotelName(hotel.getHotelName());
            response.setHotelAddress(hotel.getAddress());
            response.setHotelStarRating(hotel.getStarRating());
            response.setHotelPhoneNumber(hotel.getPhoneNumber());
            response.setHotelEmail(hotel.getEmail());
            response.setHotelWebsite(hotel.getWebsite());
            response.setIsHotelActive(hotel.getIsActive());
            response.setHotelPropertyType(hotel.getPropertyType());

            // Hotel location info
            if (hotel.getLocation() != null) {
                response.setHotelLocationCity(hotel.getLocation().getCityName());
                response.setHotelLocationDistrict(hotel.getLocation().getCityName());
            }

            // Hotel rating calculation
            if (hotel.getReviews() != null && !hotel.getReviews().isEmpty()) {
                Double avgRating = hotel.getReviews().stream()
                        .mapToDouble(review -> review.getRating().doubleValue())
                        .average()
                        .orElse(0.0);
                response.setHotelAverageRating(avgRating);
                response.setHotelReviewCount(hotel.getReviews().size());
            } else {
                response.setHotelAverageRating(0.0);
                response.setHotelReviewCount(0);
            }

            // Hotel images
            if (hotel.getImages() != null && !hotel.getImages().isEmpty()) {
                List<HotelImageResponse> imageResponses = hotel.getImages().stream()
                        .map(image -> {
                            HotelImageResponse imageResponse = modelMapper.map(image, HotelImageResponse.class);
                            imageResponse.setHotelName(hotel.getHotelName()); // Set hotel name in image
                            return imageResponse;
                        })
                        .collect(Collectors.toList());
                response.setHotelImages(imageResponses);

                // Set primary image URL for easy access
                response.setPrimaryHotelImageUrl(response.getPrimaryHotelImageUrl());
            }
        }

        // ========== ✅ NEW: ROOM TYPE DETAILS ==========
        response.setRoomTypeBasePrice(roomType.getBasePrice() != null ? roomType.getBasePrice().doubleValue() : null);
        response.setRoomTypeMaxOccupancy(roomType.getMaxOccupancy());
        response.setRoomTypeDescription(roomType.getDescription());
        response.setRoomTypeBedSize(roomType.getMaxOccupancy() != null ? roomType.getMaxOccupancy().doubleValue() : null);

        // Room type amenities (combine amenity names)
        if (roomType.getAmenities() != null && !roomType.getAmenities().isEmpty()) {
            String amenitiesString = roomType.getAmenities().stream()
                    .map(amenity -> amenity.getAmenityName())
                    .collect(Collectors.joining(", "));
            response.setRoomTypeAmenities(amenitiesString);
        }

        // ========== EXISTING PERMISSION CALCULATIONS ==========
        response.setCanCancel(calculateCanCancel(booking));
        response.setCanModify(calculateCanModify(booking));
        response.setCanCheckIn(calculateCanCheckIn(booking));
        response.setCanCheckOut(calculateCanCheckOut(booking));

        return response;
    }


    private BookingResponse mapToBookingResponse(Booking booking) {
        BookingResponse response = new BookingResponse();

        // ========== BASIC BOOKING FIELDS ==========
        response.setId(booking.getId());
        response.setUserId(booking.getUser().getId());
        response.setRoomTypeId(booking.getRoomType().getId());
        response.setCheckInDate(booking.getCheckInDate());
        response.setCheckOutDate(booking.getCheckOutDate());
        response.setNumberOfGuests(booking.getNumberOfGuests());
        response.setBookingDate(booking.getBookingDate());
        response.setTotalPrice(booking.getTotalPrice().doubleValue());
        response.setSpecialRequests(booking.getSpecialRequests());
        response.setStatus(booking.getStatus());

        // Room and user info
        if (booking.getAssignedRoom() != null) {
            response.setAssignedRoomId(booking.getAssignedRoom().getId());
            response.setRoomNumber(booking.getAssignedRoom().getRoomNumber());
        }

        response.setRoomTypeName(booking.getRoomType().getTypeName());
        response.setUserName(booking.getUser().getUsername());
        response.setUserEmail(booking.getUser().getEmail());

        // ========== ✅ NEW: ENHANCED HOTEL INFORMATION ==========
        RoomType roomType = booking.getRoomType();
        Hotel hotel = roomType.getHotel();

        if (hotel != null) {
            // Basic hotel info
            response.setHotelId(hotel.getId());
            response.setHotelName(hotel.getHotelName());
            response.setHotelAddress(hotel.getAddress());
            response.setHotelStarRating(hotel.getStarRating());
            response.setHotelPhoneNumber(hotel.getPhoneNumber());
            response.setHotelEmail(hotel.getEmail());
            response.setHotelWebsite(hotel.getWebsite());
            response.setIsHotelActive(hotel.getIsActive());
            response.setHotelPropertyType(hotel.getPropertyType());

            // Hotel location info
            if (hotel.getLocation() != null) {
                response.setHotelLocationCity(hotel.getLocation().getCityName());
                response.setHotelLocationDistrict(hotel.getLocation().getCityName());
            }

            // Hotel rating calculation
            if (hotel.getReviews() != null && !hotel.getReviews().isEmpty()) {
                Double avgRating = hotel.getReviews().stream()
                        .mapToDouble(review -> review.getRating().doubleValue())
                        .average()
                        .orElse(0.0);
                response.setHotelAverageRating(avgRating);
                response.setHotelReviewCount(hotel.getReviews().size());
            } else {
                response.setHotelAverageRating(0.0);
                response.setHotelReviewCount(0);
            }

            // Hotel images with proper mapping
            if (hotel.getImages() != null && !hotel.getImages().isEmpty()) {
                List<HotelImageResponse> imageResponses = hotel.getImages().stream()
                        .map(image -> {
                            HotelImageResponse imageResponse = new HotelImageResponse();
                            imageResponse.setId(image.getId());
                            imageResponse.setHotelId(hotel.getId());
                            imageResponse.setImageUrl(image.getImageUrl());
                            imageResponse.setCaption(image.getCaption());
                            imageResponse.setIsPrimary(image.getIsPrimary());
                            imageResponse.setHotelName(hotel.getHotelName());
                            return imageResponse;
                        })
                        .collect(Collectors.toList());
                response.setHotelImages(imageResponses);

                // Set primary image URL for easy access
                response.setPrimaryHotelImageUrl(response.getPrimaryHotelImageUrl());
            }
        }

        // ========== ✅ NEW: ROOM TYPE DETAILS ==========
        response.setRoomTypeBasePrice(roomType.getBasePrice() != null ? roomType.getBasePrice().doubleValue() : null);
        response.setRoomTypeMaxOccupancy(roomType.getMaxOccupancy());
        response.setRoomTypeDescription(roomType.getDescription());

        // Room type amenities (combine amenity names)
        if (roomType.getAmenities() != null && !roomType.getAmenities().isEmpty()) {
            String amenitiesString = roomType.getAmenities().stream()
                    .map(amenity -> amenity.getAmenityName())
                    .collect(Collectors.joining(", "));
            response.setRoomTypeAmenities(amenitiesString);
        }

        // ========== PERMISSION CALCULATIONS ==========
        response.setCanCancel(canCancelBooking(booking));
        response.setCanModify(canCancelBooking(booking)); // Same rules as cancel

        return response;
    }



}