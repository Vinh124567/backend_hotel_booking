package com.example.demo.exception;

public enum ErrorCode {
    // Lỗi chung
    EXITS(406, "Đã tồn tại"),
    NOT_EXITS(407, "Không tồn tại"),
    NOT_FOUND_ID(408, "Không tìm thấy ID"),
    NOT_FOUND(404, "Không tìm thấy"),

    // Lỗi phòng ban
    DEPARTMENT_NAME_EXISTED(409, "Tên phòng ban đã tồn tại"),
    DEPARTMENT_NOT_FOUND(404, "Không tìm thấy phòng ban"),

    // Lỗi vị trí
    POSITION_NOT_FOUND(404, "Không tìm thấy vị trí"),
    POSITION_NAME_EXIST(409, "Tên vị trí đã tồn tại"),

    // Lỗi phụ cấp
    ALLOWANCE_NOT_FOUND(404, "Không tìm thấy phụ cấp"),

    // Lỗi tên
    NAME_EXISTED(409, "Tên đã tồn tại"),

    // Lỗi hợp đồng
    NOT_FOUND_ID_CONTRACT(404, "Không tìm thấy ID hợp đồng"),

    // Lỗi lương
    NOT_FOUND_ID_SALARY(404, "Không tìm thấy ID lương"),

    // Lỗi xác thực và phân quyền
    UNAUTHENTICATED(401, "Sai tên đăng nhập hoặc mật khẩu"),
    UNAUTHORIZED(403, "Bạn không có quyền truy cập"),

    // Lỗi người dùng
    USER_EXISTED(409, "Người dùng đã tồn tại"),
    USER_NOT_EXISTED(404, "Người dùng không tồn tại"),
    USERNAME_INVALID(400, "Tên đăng nhập phải có ít nhất 3 ký tự"),
    INVALID_PASSWORD(400, "Mật khẩu phải có ít nhất 6 ký tự"),

    // Lỗi đổi mật khẩu
    PASSWORD_MISMATCH(400, "Mật khẩu mới và xác nhận mật khẩu không khớp"),
    INVALID_CURRENT_PASSWORD(400, "Mật khẩu hiện tại không đúng"),
    SAME_PASSWORD(400, "Mật khẩu mới phải khác mật khẩu hiện tại"),
    PASSWORD_TOO_SHORT(400, "Mật khẩu phải có ít nhất 6 ký tự"),
    PASSWORD_INVALID_FORMAT(400, "Mật khẩu phải chứa ít nhất 1 chữ cái và 1 số"),

    // Lỗi token
    INVALID_TOKEN(401, "Token không hợp lệ"),
    TOKEN_EXPIRED(401, "Token đã hết hạn"),

    // Lỗi validation
    INVALID_INPUT(400, "Dữ liệu đầu vào không hợp lệ"),
    MISSING_REQUIRED_FIELD(400, "Thiếu trường bắt buộc"),

    // Lỗi hệ thống
    INTERNAL_SERVER_ERROR(500, "Lỗi hệ thống"),
    SERVICE_UNAVAILABLE(503, "Dịch vụ tạm thời không khả dụng");

    private int code;
    private String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}