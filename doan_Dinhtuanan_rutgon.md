# TÓM TẮT ĐỒ ÁN TỐT NGHIỆP

---

## 1. Thông tin sinh viên
- **Họ và tên:** Đinh Tuấn An
- **Mã sinh viên:** 73DCTT23376
- **Lớp:** 73DCTT21
- **Ngành đào tạo:** Công nghệ Thông tin
- **Khoa:** Công nghệ Thông tin
- **Giảng viên hướng dẫn:** ThS. Đoàn Thị Thanh Hằng

---

## 2. Tên đề tài
**"Xây dựng website quản lý quán cafe THECOFFEE247 và tìm kiếm món ăn hot trend hiện tại"**

---

## 3. Mục tiêu của đề tài
Đề tài hướng tới việc giải quyết các bài toán vận hành thực tiễn của quán cafe THECOFFEE247 thông qua các mục tiêu chính sau:
- **Tối ưu hóa quy trình quản lý và bán hàng:** Xây dựng hệ thống phần mềm quản lý bán hàng (POS) toàn diện tại quầy giúp nhân viên thực hiện order, phục vụ tại bàn hoặc mang đi một cách nhanh chóng, chính xác, hạn chế tối đa sai sót thủ công trong giờ cao điểm.
- **Tích hợp thanh toán đa phương thức hiện đại:** Hỗ trợ khách hàng thanh toán linh hoạt qua tiền mặt, mã QR động MoMo (Dynamic QR) và cổng thanh toán thẻ quốc tế PayPal, nâng cao trải nghiệm mua sắm và tự động hóa quy trình đối soát hóa đơn.
- **Ứng dụng Trí tuệ Nhân tạo (AI) hỗ trợ kinh doanh:** Tích hợp tính năng phân tích dữ liệu và tìm kiếm các món ăn "hot trend" từ internet và mạng xã hội dựa trên mô hình ngôn ngữ lớn (Llama AI) để gợi ý cho người quản lý cập nhật thực đơn kịp thời, bắt kịp xu hướng thị trường nhằm thu hút khách hàng mới.
- **Nâng cao khả năng quản trị:** Cung cấp cho người quản lý hệ thống báo cáo thống kê trực quan về doanh thu, số lượng đơn hàng, top sản phẩm bán chạy theo ngày/tuần/tháng/năm để đưa ra các chiến lược kinh doanh phù hợp.
- **Chăm sóc khách hàng thân thiết:** Xây dựng cơ chế tích lũy điểm thưởng dựa trên số điện thoại khi thanh toán và cho phép khách hàng đổi điểm tích lũy lấy các mã giảm giá (voucher) sử dụng cho lần mua tiếp theo.

---

## 4. Giới hạn và phạm vi của đề tài
- **Phạm vi nghiên cứu nghiệp vụ:** Tập trung nghiên cứu quy trình vận hành và bán hàng của quán cafe THECOFFEE247 bao gồm: quản lý thực đơn (món ăn, danh mục), quản lý bàn phục vụ (trạng thái trống/có khách), quy trình gọi món và quản lý đơn hàng (POS), quy trình áp dụng voucher và tính điểm, quy trình xuất hóa đơn và thanh toán, quy trình phân tích xu hướng thị trường thông qua AI.
- **Phạm vi đối tượng sử dụng:**
  - **Khách hàng (Client):** Xem danh mục thực đơn trực tuyến, quản lý tài khoản cá nhân, theo dõi lịch sử tích điểm, thực hiện đổi điểm lấy voucher và gửi ý kiến phản hồi hoặc đề xuất món ăn mới đến quán.
  - **Nhân viên bán hàng (Staff):** Theo dõi sơ đồ bàn theo thời gian thực, thực hiện tạo đơn đặt món (dine-in hoặc takeaway), chỉnh sửa số lượng món, áp dụng mã voucher cho khách hàng, hỗ trợ tích điểm, xử lý thanh toán và in xuất hóa đơn.
  - **Người quản lý (Admin/Manager):** Quản trị danh sách nhân sự, phân quyền người dùng, quản trị danh mục và thực đơn món ăn, quản trị sơ đồ bàn, thiết lập các chương trình khuyến mãi (voucher), xem dashboard báo cáo doanh thu chi tiết và sử dụng công cụ AI phân tích hot trend để đưa ra quyết định cập nhật menu.
- **Giới hạn công nghệ áp dụng:**
  - **Frontend:** Next.js (phiên bản 14+ sử dụng App Router), ReactJS, Tailwind CSS hỗ trợ thiết kế giao diện responsive và động, ngôn ngữ TypeScript tăng cường an toàn kiểu dữ liệu.
  - **Backend:** Java Spring Boot xây dựng hệ thống RESTful API, tích hợp Spring Security và JWT để bảo mật thông tin và phân quyền hệ thống. Tích hợp đăng nhập nhanh qua Google OAuth2.
  - **Cơ sở dữ liệu:** PostgreSQL lưu trữ dữ liệu quan hệ ổn định, thực hiện ràng buộc toàn vẹn và giao dịch an toàn.
  - **Mô hình AI:** Tích hợp mô hình Llama AI (thông qua API/Ollama) kết hợp với công cụ thu thập thông tin trực tuyến (SerpAPI) để phân tích các từ khóa về món ăn đang thịnh hành.

---

## 5. Nội dung thực hiện
Sinh viên đã tiến hành nghiên cứu và thực hiện đồ án thông qua các giai đoạn cụ thể:

### 5.1. Khảo sát nghiệp vụ thực tế
Tiến hành phỏng vấn trực tiếp nhân viên và quản lý tại quán THECOFFEE247 nhằm hiểu rõ quy trình vận hành hiện tại:
- **Khảo sát nhân viên bán hàng:** Ghi nhận quy trình đón tiếp khách, ghi order thủ công bằng giấy, chuyển thông tin xuống quầy pha chế, tiếp nhận nhiều hình thức thanh toán và các khó khăn gặp phải khi quán đông khách (dễ nhầm lẫn hóa đơn, chậm trễ cập nhật trạng thái bàn trống).
- **Khảo sát người quản lý:** Tìm hiểu quy trình quản lý ca trực, kiểm kê doanh thu cuối ngày bằng cách cộng sổ thủ công, quản lý kho nguyên liệu thô sơ và nhu cầu cần có báo cáo doanh số tự động theo chu kỳ thời gian thực, cũng như công cụ hỗ trợ cập nhật thực đơn theo xu hướng của giới trẻ.

### 5.2. Mô tả bài toán và đề xuất giải pháp
Từ kết quả khảo sát, đồ án đề xuất giải pháp chuyển đổi số toàn diện hoạt động của quán từ ghi chép thủ công sang vận hành trên nền tảng Website. Hệ thống hóa bài toán thành 9 phân hệ chức năng cốt lõi:
1. *Quản lý xác thực:* Đăng ký, đăng nhập nội bộ (Local) hoặc Google OAuth2, quên mật khẩu và khôi phục tài khoản qua OTP gửi về Email.
2. *Quản lý người dùng:* Tạo lập tài khoản, quản lý hồ sơ cá nhân và phân quyền truy cập giữa Admin và Staff.
3. *Quản lý thực đơn:* Quản lý thông tin chi tiết món ăn (tên, giá, hình ảnh, mô tả).
4. *Quản lý danh mục:* Phân loại các nhóm đồ uống và đồ ăn để dễ tìm kiếm.
5. *Quản lý bàn phục vụ:* Sơ đồ hóa các khu vực bàn, quản lý trạng thái hoạt động trực quan.
6. *Quản lý đơn hàng (POS):* Tạo đơn, gọi thêm món, hủy món, cập nhật trạng thái chế biến.
7. *Quản lý thanh toán:* Tích hợp tính toán hóa đơn tự động kèm chiết khấu voucher, kết nối API MoMo tạo mã QR thanh toán động và cổng PayPal xử lý thẻ tín dụng.
8. *Quản lý khuyến mãi/Voucher:* Quản lý kho voucher giảm giá theo phần trăm hoặc số tiền cố định, thiết lập số điểm tích lũy tối thiểu để đổi.
9. *Thống kê báo cáo:* Dashboard phân tích doanh thu bằng biểu đồ đường trực quan, thống kê top món bán chạy và doanh số theo thời gian.

### 5.3. Thiết kế hệ thống
- **Sơ đồ Use Case tổng quan:** Mô tả mối liên kết giữa các tác nhân (Khách hàng, Nhân viên, Quản lý) với các chức năng tương ứng của hệ thống.
- **Sơ đồ tuần tự (Sequence Diagram):** Đặc tả chi tiết tiến trình tương tác giữa giao diện người dùng (Frontend), bộ điều khiển (Backend Controller), dịch vụ nghiệp vụ (Service), và cơ sở dữ liệu (Database) cho các chức năng nhạy cảm như Đăng nhập, Tạo đơn hàng, Thanh toán qua MoMo Webhook, và AI gợi ý xu hướng.
- **Sơ đồ hoạt động (Activity Diagram):** Biểu diễn luồng xử lý chi tiết của các nghiệp vụ phức tạp như quy trình Đổi điểm tích lũy lấy voucher, luồng thanh toán hóa đơn và giải phóng bàn phục vụ.
- **Sơ đồ trạng thái (State Diagram):** Định nghĩa rõ các chuyển đổi trạng thái của:
    - *Bàn phục vụ:* Trống (Available) $\leftrightarrow$ Đang phục vụ (Occupied).
    - *Đơn hàng:* Chờ xử lý (Pending) $\rightarrow$ Đang phục vụ (Served) / Đã thanh toán (Paid) / Đã hủy (Cancelled).
    - *Voucher:* Chưa sử dụng (Active) $\rightarrow$ Đã sử dụng (Used) / Hết hạn (Expired).

- **Thiết kế Cơ sở dữ liệu:** Hệ thống CSDL PostgreSQL được thiết kế chuẩn hóa với sơ đồ quan hệ ERD gồm 11 bảng dữ liệu chính:

| STT | Tên bảng | Khóa chính | Khóa ngoại | Mô tả chức năng |
| :--- | :--- | :--- | :--- | :--- |
| 1 | `users` | `id` | - | Lưu tài khoản (Email, mật khẩu băm BCrypt, điểm tích lũy, nhà cung cấp LOCAL/GOOGLE). |
| 2 | `roles` | `id` | - | Lưu danh mục vai trò người dùng (ADMIN, MANAGER, STAFF, CUSTOMER). |
| 3 | `user_roles` | `user_id`, `role_id` | `user_id`, `role_id` | Bảng trung gian phân chia quyền hạn (Quan hệ Many-to-Many). |
| 4 | `categories` | `id` | - | Lưu danh mục phân loại thực đơn (Cà phê, Trà, Bánh...). |
| 5 | `menu_items` | `id` | `category_id` | Chi tiết các món ăn, đồ uống (tên, giá tiền, ảnh minh họa). |
| 6 | `restaurant_tables` | `id` | - | Quản lý các bàn phục vụ (Tên bàn, sức chứa, trạng thái trống). |
| 7 | `orders` | `id` | `user_id`, `table_id` | Lưu thông tin đơn hàng, thời gian order, loại đơn (DINE_IN/TAKEAWAY). |
| 8 | `order_items` | `id` | `order_id`, `menu_item_id` | Chi tiết số lượng của từng món ăn trong mỗi đơn hàng. |
| 9 | `invoices` | `id` | `order_id`, `user_id` | Ghi nhận hóa đơn thanh toán (tổng tiền gốc, giảm giá, số tiền thực trả, phương thức thanh toán). |
| 10 | `vouchers` | `id` | - | Lưu trữ thông tin mã giảm giá, mức giảm, điểm yêu cầu để đổi. |
| 11 | `user_vouchers` | `id` | `user_id`, `voucher_id` | Quản lý danh sách voucher cá nhân mà khách hàng đã đổi được. |

---

## 6. Kết quả thực hiện
Sau quá trình thực hiện đồ án, sinh viên đã hoàn thành xây dựng sản phẩm phần mềm chạy thực tế với các giao diện cốt lõi và hoàn thành quy trình kiểm thử hệ thống.

### 6.1. Kết quả sản phẩm phần mềm
- **Giao diện Client dành cho khách hàng:** Thiết kế hiện đại, responsive hoàn toàn trên điện thoại và máy tính. Khách hàng có thể dễ dàng duyệt thực đơn, xem thông tin liên hệ, đăng ký thành viên để bắt đầu tích lũy điểm. Hệ thống cung cấp trang cá nhân hiển thị số điểm hiện có và cho phép đổi điểm lấy mã voucher chỉ với một cú nhấp chuột.
- **Giao diện POS dành cho nhân viên bán hàng:** Sơ đồ bàn hiển thị trực quan thông qua các khối màu sắc đại diện cho trạng thái (màu xanh lá: bàn trống, màu đỏ: bàn đang có khách). Nhân viên click vào bàn để thực hiện thêm món nhanh từ thực đơn, điều chỉnh số lượng, nhập số điện thoại tích điểm của khách hàng, áp dụng voucher giảm giá và tiến hành in hóa đơn.
- **Giao diện quản lý thống kê doanh thu (Admin/Manager):** Cung cấp các biểu đồ thống kê dạng đường (Line chart) thể hiện biến động doanh thu theo ngày trong tháng, biểu đồ hình tròn hiển thị tỉ lệ thanh toán giữa các phương thức (Tiền mặt, MoMo, PayPal). Thống kê chi tiết danh sách các món ăn bán chạy nhất để hỗ trợ quản lý dự báo kế hoạch nhập kho.
- **Hệ thống tương tác AI tìm kiếm Trend:** Giao diện hỗ trợ quản lý gửi yêu cầu phân tích xu hướng món ăn. Hệ thống sử dụng mô hình AI quét dữ liệu từ internet và trả về các món ăn đang là xu hướng kèm mô tả lý do hot trend. Quản lý có thể thêm nhanh món ăn đó vào thực đơn của quán trực tiếp từ kết quả phân tích của AI.

### 6.2. Tài liệu kiểm thử hệ thống (UAT)
Quá trình kiểm thử chức năng được tiến hành nghiêm ngặt qua 22 ca kiểm thử (Test Cases) chính. Kết quả kiểm thử cụ thể như sau:

| Mã TC | Nhóm chức năng | Kịch bản kiểm thử | Kết quả mong đợi | Kết quả thực tế | Trạng thái |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC_01** | Xác thực | Đăng nhập tài khoản Local thành công | Chuyển hướng vào Dashboard tương ứng vai trò | Đúng mong đợi | **PASS** |
| **TC_02** | Xác thực | Đăng nhập thất bại do sai mật khẩu | Hiển thị thông báo "Mật khẩu không chính xác" | Đúng mong đợi | **PASS** |
| **TC_03** | Xác thực | Đăng nhập qua Google OAuth2 | Đăng nhập thành công, đồng bộ tài khoản Google | Đúng mong đợi | **PASS** |
| **TC_04** | Xác thực | Đăng ký tài khoản khách hàng mới | Tạo tài khoản thành công, chuyển về trang đăng nhập | Đúng mong đợi | **PASS** |
| **TC_05** | Xác thực | Đăng ký thất bại do trùng Email | Hiển thị thông báo "Email này đã được sử dụng" | Đúng mong đợi | **PASS** |
| **TC_06** | Xác thực | Quên mật khẩu - Yêu cầu gửi mã OTP | Gửi mã OTP 6 số về email đăng ký thành công | Đúng mong đợi | **PASS** |
| **TC_07** | Xác thực | Đổi mật khẩu mới qua mã OTP hợp lệ | Hệ thống đổi mật khẩu thành công và cho đăng nhập | Đúng mong đợi | **PASS** |
| **TC_08** | Xác thực | Đăng xuất khỏi hệ thống | Xóa token phiên làm việc, quay về trang đăng nhập | Đúng mong đợi | **PASS** |
| **TC_09** | Vận hành | Hiển thị trạng thái sơ đồ bàn phục vụ | Bàn trống hiển thị màu xanh, bàn có khách màu đỏ | Đúng mong đợi | **PASS** |
| **TC_10** | Vận hành | Tạo đơn hàng POS tại chỗ (Dine-in) | Bàn chuyển sang màu đỏ, tạo đơn hàng PENDING | Đúng mong đợi | **PASS** |
| **TC_11** | Vận hành | Tạo đơn hàng mang đi (Takeaway) | Tạo đơn hàng thành công, không chiếm dụng bàn | Đúng mong đợi | **PASS** |
| **TC_12** | Vận hành | Thêm/Sửa/Xóa số lượng món ăn trong đơn | Số lượng món cập nhật, tính lại tổng tiền hóa đơn | Đúng mong đợi | **PASS** |
| **TC_13** | Vận hành | Áp dụng mã Voucher giảm giá hợp lệ | Giảm trừ số tiền thanh toán theo cấu hình voucher | Đúng mong đợi | **PASS** |
| **TC_14** | Vận hành | Thanh toán đơn hàng bằng Tiền mặt | Đơn hàng đổi thành PAID, in hóa đơn và giải phóng bàn | Đúng mong đợi | **PASS** |
| **TC_15** | Vận hành | Thanh toán đơn hàng bằng cổng MoMo | Hiển thị mã QR, nhận Webhook xác nhận PAID sau quét | Đúng mong đợi | **PASS** |
| **TC_16** | Quản trị | Thêm mới tài khoản nhân viên (Staff) | Tài khoản nhân viên mới được tạo và hiển thị trong danh sách | Đúng mong đợi | **PASS** |
| **TC_17** | Quản trị | Vô hiệu hóa (Khóa) tài khoản nhân viên | Tài khoản bị khóa không thể đăng nhập vào hệ thống | Đúng mong đợi | **PASS** |
| **TC_18** | Quản trị | Thêm món ăn mới vào thực đơn | Món ăn mới hiển thị trên giao diện gọi món POS | Đúng mong đợi | **PASS** |
| **TC_19** | Quản trị | Xóa món ăn đang nằm trong đơn chưa thanh toán | Hệ thống báo lỗi khóa ngoại, từ chối hành động xóa | Đúng mong đợi | **PASS** |
| **TC_20** | Quản trị | Thêm mới bàn phục vụ vào sơ đồ | Bàn mới hiển thị ngay lập tức trên sơ đồ quản lý | Đúng mong đợi | **PASS** |
| **TC_21** | Quản trị | Xem Dashboard thống kê doanh thu | Biểu đồ doanh thu và top món bán chạy tải chính xác | Đúng mong đợi | **PASS** |
| **TC_22** | Tương tác AI | Kích hoạt AI gợi ý tìm kiếm món ăn Hot Trend | Gọi Llama AI phân tích và trả về danh sách món xu hướng | Đúng mong đợi | **PASS** |

---

## 7. Kết luận và hướng phát triển

### 7.1. Kết quả đạt được
- **Giải pháp thực tiễn cao:** Đã số hóa thành công quy trình quản lý quán cafe từ thủ công sang tự động. Hệ thống POS hoạt động ổn định, mượt mà trên nhiều thiết bị giúp tối ưu hóa thời gian đặt món và in hóa đơn tại quầy.
- **Tích hợp thanh toán đáng tin cậy:** Triển khai thành công hình thức thanh toán QR động qua MoMo và cổng thanh toán PayPal, giúp khách hàng thanh toán tiện lợi và rút ngắn thời gian đối soát cho quán.
- **Tính đột phá trong ứng dụng AI:** Việc tích hợp mô hình Llama AI để tìm kiếm món ăn hot trend là một hướng đi mới mẻ và có giá trị thực tiễn lớn, hỗ trợ ban quản lý nhanh chóng cập nhật thực đơn theo xu hướng tiêu dùng hiện đại.
- **Hệ thống tối ưu:** Cơ sở dữ liệu PostgreSQL được thiết kế tối ưu hóa chỉ mục, giúp việc truy xuất lịch sử hóa đơn và kết xuất dữ liệu doanh thu diễn ra trong thời gian thực với độ trễ thấp.

### 7.2. Hướng phát triển trong tương lai
Để hệ thống hoàn thiện và ứng dụng rộng rãi hơn, đồ án đề xuất một số hướng phát triển tiếp theo:
- **Xây dựng ứng dụng di động (Mobile App):** Phát triển thêm ứng dụng trên nền tảng Android/iOS dành riêng cho khách hàng để họ chủ động xem menu, đặt món trước khi đến quán và theo dõi thẻ thành viên tích điểm một cách thuận tiện nhất.
- **Mở rộng các cổng thanh toán nội địa:** Tích hợp thêm các hình thức thanh toán trực tuyến phổ biến khác tại Việt Nam như ZaloPay, VNPAY và Apple Pay.
- **Nâng cấp mô hình AI quản lý kho:** Nghiên cứu và huấn luyện mô hình AI nâng cao có khả năng phân tích dữ liệu lịch sử bán hàng kết hợp với thời tiết, mùa vụ để đưa ra dự báo chính xác số lượng nguyên liệu thô cần nhập kho hàng tuần, giúp quán cafe tối ưu chi phí vận hành và giảm thiểu hao hụt.
