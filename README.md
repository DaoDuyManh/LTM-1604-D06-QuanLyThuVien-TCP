<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
    Quản lý sách – thư viện qua mạng
</h2>
<div align="center">
    <p align="center">
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>

<!doctype html>
<html lang="vi">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width,initial-scale=1" />
  <title>README — Hệ thống Quản lý Thư viện</title>
  <style>
    :root{
      --bg:#0f1724;
      --card:#0b1220;
      --muted:#98a0b3;
      --accent:#6ee7b7;
      --accent-2:#60a5fa;
      --glass: rgba(255,255,255,0.03);
      --glass-2: rgba(255,255,255,0.02);
      --radius:14px;
      --maxw:1000px;
      --fw-sans: "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    }
    *{box-sizing:border-box}
    html,body{height:100%;}
    body{
      margin:0;
      font-family:var(--fw-sans);
      background: linear-gradient(180deg, #071023 0%, #071428 40%), radial-gradient(800px 400px at 10% 10%, rgba(96,165,250,0.04), transparent 10%), radial-gradient(600px 300px at 90% 90%, rgba(110,231,183,0.03), transparent 10%);
      color:#e6eef8;
      -webkit-font-smoothing:antialiased;
      -moz-osx-font-smoothing:grayscale;
      padding:40px 20px;
      display:flex;
      align-items:flex-start;
      justify-content:center;
    }
    .wrap{
      width:100%;
      max-width:var(--maxw);
      background:linear-gradient(180deg, rgba(255,255,255,0.02), rgba(255,255,255,0.01));
      border-radius:var(--radius);
      padding:28px;
      box-shadow: 0 10px 30px rgba(2,6,23,0.6), inset 0 1px 0 rgba(255,255,255,0.02);
      border: 1px solid rgba(255,255,255,0.03);
    }

    header{
      display:flex;
      gap:16px;
      align-items:center;
      margin-bottom:18px;
    }
    .logo{
      width:64px;
      height:64px;
      border-radius:12px;
      background:linear-gradient(135deg,var(--accent-2),var(--accent));
      display:grid;
      place-items:center;
      font-weight:700;
      font-size:22px;
      color:#05233a;
      box-shadow: 0 6px 18px rgba(8,20,40,0.6);
    }
    h1{
      margin:0;
      font-size:20px;
      letter-spacing:0.2px;
    }
    p.lead{
      margin:6px 0 0;
      color:var(--muted);
      font-size:13px;
    }
    .grid{
      display:grid;
      grid-template-columns: 1fr 360px;
      gap:20px;
      margin-top:18px;
    }
    @media (max-width:980px){
      .grid{grid-template-columns:1fr; }
    }

    /* main column */
    .content{
      background:var(--glass);
      padding:18px;
      border-radius:12px;
      border:1px solid rgba(255,255,255,0.02);
    }
    section{ margin-bottom:18px; }
    section h2{
      margin:0 0 8px 0;
      display:flex;
      align-items:center;
      gap:10px;
      font-size:16px;
    }
    .badge{
      background:rgba(255,255,255,0.03);
      padding:6px 10px;
      border-radius:999px;
      color:var(--muted);
      font-size:13px;
      border:1px solid rgba(255,255,255,0.02);
    }
    .card{
      background:var(--glass-2);
      padding:14px;
      border-radius:10px;
      border:1px solid rgba(255,255,255,0.02);
      color:#dbeafe;
      line-height:1.5;
      font-size:14px;
    }

    ul.tech{
      list-style:none;
      padding:0;
      margin:8px 0 0 0;
      display:grid;
      grid-template-columns:repeat(2, minmax(0,1fr));
      gap:8px;
    }
    ul.tech li{
      background:rgba(255,255,255,0.02);
      padding:8px 10px;
      border-radius:8px;
      font-size:13px;
      color:var(--muted);
      border:1px solid rgba(255,255,255,0.01);
    }

    .sidebar{
      display:flex;
      flex-direction:column;
      gap:12px;
      align-items:stretch;
    }
    .contact{
      padding:14px;
      border-radius:10px;
      background: linear-gradient(180deg, rgba(6,10,22,0.6), rgba(6,10,22,0.5));
      border:1px solid rgba(255,255,255,0.02);
    }
    .contact h3{ margin:0 0 8px 0; font-size:15px; }
    .contact p{ margin:6px 0; color:var(--muted); font-size:13px; }

    .cta{
      display:flex;
      gap:10px;
      margin-top:10px;
    }
    .btn{
      display:inline-flex;
      align-items:center;
      gap:8px;
      padding:10px 12px;
      border-radius:10px;
      text-decoration:none;
      cursor:pointer;
      border: none;
      font-weight:600;
      font-size:13px;
    }
    .btn.primary{
      background:linear-gradient(90deg,var(--accent),var(--accent-2));
      color:#05233a;
      box-shadow:0 8px 20px rgba(96,165,250,0.08);
    }
    .btn.ghost{
      background:transparent;
      color:var(--muted);
      border:1px solid rgba(255,255,255,0.03);
    }

    pre.code{
      background:rgba(0,0,0,0.25);
      padding:12px;
      border-radius:8px;
      overflow:auto;
      color:#e6eef8;
      font-size:13px;
      border:1px solid rgba(255,255,255,0.02);
    }

    footer{
      margin-top:18px;
      text-align:center;
      color:var(--muted);
      font-size:13px;
    }

    .pill{
      display:inline-block;
      padding:6px 10px;
      background:rgba(255,255,255,0.02);
      border-radius:999px;
      color:var(--muted);
      font-size:13px;
      border:1px solid rgba(255,255,255,0.01);
    }

  </style>
</head>
<body>
  <div class="wrap" role="main">
    <header>
      <div class="logo">📚</div>
      <div>
        <h1>Hệ thống Quản lý Thư viện</h1>
        <p class="lead">Ứng dụng client-server đơn giản cho quản lý sách, mượn/trả và quản trị viên.</p>
      </div>
    </header>

    <div class="grid">
      <!-- main -->
      <div class="content">
        <!-- 1. Giới thiệu -->
        <section id="intro">
          <h2>📖 1. Giới thiệu hệ thống <span class="badge">Mục tiêu</span></h2>
          <div class="card">
            Hệ thống Quản lý Thư viện giúp bạn quản lý dữ liệu sách và người dùng theo mô hình client-server:
            <ul style="margin-top:8px;">
              <li>Người dùng: đăng ký, đăng nhập, xem danh sách sách, mượn & trả sách.</li>
              <li>Server: lưu trữ dữ liệu (file text), xử lý lệnh (REGISTER / LOGIN / LIST_BOOKS / BORROW / RETURN / SEARCH / MYBOOKS).</li>
              <li>Quản trị (AdminUI): quản lý sách, chỉnh số lượng, xem ai đang mượn, quản lý user và log hệ thống.</li>
            </ul>
            <p style="margin-top:10px;color:var(--muted)">
              Thiết kế hướng tới bài tập lớn hoặc prototype cho thư viện nhỏ. Dữ liệu hiện lưu trên file text (dễ sửa, dễ kiểm thử).
            </p>
          </div>
        </section>

        <!-- 2. Công nghệ -->
        <section id="tech">
          <h2>🔧 2. Các công nghệ được sử dụng</h2>
          <div class="card">
            Ứng dụng được xây dựng hoàn toàn bằng Java với các thành phần chính:
            <ul class="tech" style="margin-top:8px;">
              <li>Java SE (Socket TCP)</li>
              <li>Swing (Java GUI) — MainUI, AdminUI, LoginUI</li>
              <li>File I/O (java.nio.file) — lưu `accounts.txt`, `books.txt`</li>
              <li>Mô hình đơn giản: <strong>model / server / ui / client</strong></li>
              <li>Định dạng lưu: sách `title|author|category|quantity|borrower1,borrower2...`</li>
              <li>Hệ thống phù hợp cho demo, bài tập và prototyping</li>
            </ul>

            <div style="margin-top:12px">
              Ví dụ nhanh (gọi API nội bộ giữa client ↔ server):
              <pre class="code">LIST_BOOKS
MYBOOKS:username
BORROW:username:Book Title
RETURN:username:Book Title
SEARCH:keyword</pre>
            </div>
          </div>
        </section>

        <!-- 5. Contact -->
        <section id="contact">
          <h2>✉️ 5. Liên hệ</h2>
          <div class="card">
            <p>Nếu bạn cần hỗ trợ, sửa lỗi, hoặc muốn mở rộng hệ thống (ví dụ: dùng DB, REST API hoặc Web UI), liên hệ theo thông tin dưới:</p>

            <div style="display:flex;gap:12px;flex-wrap:wrap;margin-top:8px">
              <div><span class="pill">Tác giả:</span> <strong>[Tên của bạn]</strong></div>
              <div><span class="pill">Email:</span> <strong>you@example.com</strong></div>
              <div><span class="pill">SĐT:</span> <strong>+84 9xx xxx xxx</strong></div>
              <div><span class="pill">GitHub:</span> <strong>github.com/your-username</strong></div>
            </div>

            <p style="margin-top:10px;color:var(--muted)">Ghi chú: thay thế các thông tin trên bằng thông tin thật của bạn trước khi công khai repository.</p>
          </div>
        </section>

      </div>

      <!-- sidebar -->
      <aside class="sidebar">
        <div class="contact">
          <h3>Những thứ bạn có thể thêm sau</h3>
          <p class="lead" style="color:var(--muted)">Mình để sẵn chỗ để bạn bổ sung:</p>
          <ul style="margin:6px 0 0 16px;color:var(--muted);">
            <li>Phần 3 — Hình ảnh demo (screenshot AdminUI / MainUI)</li>
            <li>Phần 4 — Hướng dẫn cài đặt & chạy (mẫu file, command javac/java)</li>
            <li>Ví dụ file `books.txt` / `accounts.txt` mẫu để test nhanh</li>
          </ul>

          <div class="cta">
            <button class="btn primary" onclick="alert('Hãy copy file này và sửa nội dung contact!')">Sao chép file</button>
            <a class="btn ghost" href="#intro">Xem phần giới thiệu</a>
          </div>
        </div>

        <div class="contact">
          <h3>Mẫu nhanh — file books.txt</h3>
          <pre class="code">The Kite Runner|Khaled Hosseini|Tiểu thuyết|14|
Things Fall Apart|Chinua Achebe|Tiểu thuyết|10|alice,bob</pre>
          <p style="color:var(--muted);font-size:13px;margin-top:8px">Format: <code>title|author|category|quantity|borrower1,borrower2...</code></p>
        </div>

        <div class="contact">
          <h3>Tối ưu đề xuất</h3>
          <ul style="margin:6px 0 0 16px;color:var(--muted);">
            <li>Sử dụng DB (SQLite/Postgres) nếu dữ liệu tăng</li>
            <li>Tách server thành REST API để dễ mở rộng</li>
            <li>Thêm logging có timestamp cho audit</li>
          </ul>
        </div>
      </aside>
    </div>

    <footer>
      <small>© <span id="year"></span> Hệ thống Quản lý Thư viện — README generated. Chỉnh sửa nội dung theo nhu cầu.</small>
    </footer>
  </div>

  <script>
    document.getElementById('year').textContent = new Date().getFullYear();
  </script>
</body>
</html>
