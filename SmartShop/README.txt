SmartShop (NetBeans Web Application - Ant)

1) Tạo project trống trong NetBeans:
   File → New Project → Java with Ant → Web → Web Application → Next…
   Project Name: SmartShop
   Server: Apache Tomcat 10.1
   Java: 17
   Finish.

2) Sao chép các thư mục trong gói này vào project vừa tạo, ghi đè:
   - src/java/…
   - web/…
   - sql/schema.sql

3) Thêm thư viện (Project Properties → Libraries → Add JAR/Folder):
   - jakarta.servlet-api-6.0.0.jar
   - jakarta.servlet.jsp.jstl-api-3.0.0.jar
   - jakarta.servlet.jsp.jstl-3.0.1.jar
   - mssql-jdbc-12.x.jre11.jar
   - jbcrypt-0.4.jar

4) Cấu hình DB (SQL Server):
   - Mở sql/schema.sql trong SSMS 19 và Execute toàn bộ.
   - Ở Tomcat set biến môi trường hoặc sửa DBContext.java:
       MSSQL_URL=jdbc:sqlserver://localhost:1433;databaseName=SmartShop;encrypt=true;trustServerCertificate=true
       MSSQL_USER=sa
       MSSQL_PASS=YourStrong!Passw0rd

5) Chạy:
   - Run project → trình duyệt mở /index.jsp → tự chuyển /home
   - Đăng ký: /register, Đăng nhập: /login, Giỏ hàng: /cart

Lưu ý: NetBeans Web Application (Ant) vẫn chạy tốt trên Tomcat 10 nếu dùng jakarta.* API ở trên.
