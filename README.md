# 💸 SpendWise — Personal Expense Tracker

A full-stack expense tracking application built with **Spring Boot**, **MongoDB**, and **Vanilla JS**.

---

## 🏗️ Architecture

```
Frontend (HTML/CSS/JS)  ──▶  Spring Boot REST API  ──▶  MongoDB
        ◀──  JWT Auth  ─────────────────────────────────────────
```

```
backend/src/main/java/com/expensetracker/
├── config/         SecurityConfig.java
├── controller/     AuthController, TransactionController, DashboardController, BudgetController
├── dto/            AuthDto, TransactionDto, BudgetDto, DashboardDto, ApiResponse
├── exception/      GlobalExceptionHandler, ResourceNotFoundException, ...
├── model/          User, Transaction, Budget
├── repository/     UserRepository, TransactionRepository, BudgetRepository
├── security/       JwtUtils, JwtAuthFilter
└── service/        AuthService, TransactionService, DashboardService, BudgetService, ExportService

frontend/
├── index.html          Login / Register
├── dashboard.html      Summary + Charts
├── transactions.html   Full CRUD + Filters + CSV Export
├── budget.html         Budget Tracking with Progress Bars
├── style.css           Shared dark theme design system
└── app.js              Shared API client, auth helpers, utilities
```

---

## ⚙️ Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| MongoDB | 6.0+ (running locally on port 27017) |

---

## 🚀 Quick Start

### 1. Start MongoDB

```bash
# macOS (Homebrew)
brew services start mongodb-community

# Ubuntu / Debian
sudo systemctl start mongod

# Windows
net start MongoDB

# Or use Docker
docker run -d -p 27017:27017 --name mongo mongo:6
```

### 2. Configure (optional)

Edit `backend/src/main/resources/application.properties`:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/expense_tracker
app.jwt.secret=your-super-secret-key-change-this-in-production
app.jwt.expiration=86400000   # 24 hours in ms
```

### 3. Run the Backend

```bash
cd backend
mvn spring-boot:run
```

API will be available at: **http://localhost:8080**

### 4. Open the Frontend

Option A — VS Code Live Server:
- Open the `frontend/` folder in VS Code
- Right-click `index.html` → "Open with Live Server"

Option B — any static file server:
```bash
cd frontend
npx serve .
# Then open http://localhost:3000
```

Option C — open directly:
```bash
# macOS
open frontend/index.html

# Linux
xdg-open frontend/index.html
```

---

## 📡 API Reference

All protected endpoints require: `Authorization: Bearer <token>`

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register new user |
| POST | `/api/auth/login` | Login, get JWT |

### Transactions
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/transactions` | List with filters + pagination |
| POST | `/api/transactions` | Create transaction |
| GET | `/api/transactions/{id}` | Get by ID |
| PUT | `/api/transactions/{id}` | Update |
| DELETE | `/api/transactions/{id}` | Delete |
| GET | `/api/transactions/recurring` | List recurring only |
| GET | `/api/transactions/export/csv` | Download CSV |

**Query params for GET /transactions:**
- `keyword`, `type`, `category`, `fromDate`, `toDate`, `minAmount`, `maxAmount`, `page`, `size`

### Dashboard
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/dashboard` | Full summary: stats, charts data, recent transactions |

### Budgets
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/budgets?month=2024-05` | Get budgets for month |
| POST | `/api/budgets` | Create/update budget |
| DELETE | `/api/budgets/{id}` | Delete budget |

---

## 🔐 Auth Flow

```
1. User registers/logs in
2. Backend returns JWT token
3. Frontend stores token in localStorage
4. All subsequent requests include: Authorization: Bearer <token>
5. Backend validates JWT on every protected route
6. Each user sees ONLY their own data
```

---

## 🎨 Features

- ✅ JWT authentication (register / login / logout)
- ✅ Full transaction CRUD
- ✅ Filter by date range, category, type, keyword, amount range
- ✅ Pagination on transaction list
- ✅ Monthly income vs expense bar chart (last 6 months)
- ✅ Category spending doughnut chart
- ✅ Top 3 spending categories with progress bars
- ✅ 5 most recent transactions on dashboard
- ✅ Monthly budget tracking per category
- ✅ Visual progress bars (green / amber / red based on usage)
- ✅ Over-budget highlighting in red
- ✅ Recurring transaction support with frequency badge
- ✅ CSV export with active filters applied
- ✅ Responsive design (mobile + desktop)
- ✅ Dark editorial theme

---

## 🗄️ MongoDB Collections

```
expense_tracker
├── users          { name, email, password (bcrypt), createdAt }
├── transactions   { userId, title, amount, type, category, date, note,
│                    isRecurring, recurringFrequency, createdAt, updatedAt }
└── budgets        { userId, category, monthlyLimit, month (YYYY-MM), createdAt }
```

---

## 🔒 Security

- Passwords hashed with **BCrypt**
- Stateless auth with **JWT (HS256)**
- CORS configured — update `app.cors.allowed-origins` in `application.properties`
- **Change `app.jwt.secret` before deploying to production!**
