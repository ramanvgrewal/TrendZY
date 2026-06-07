# TrendZY Backend: AI-Powered Trend Intelligence Engine

TrendZY is a high-performance backend system designed to discover, analyze, and track consumer trends for the Indian Gen-Z market. It automatically scrapes social signals, uses Large Language Models (LLMs) to extract buyable products, and enriches them with real-world e-commerce data.

---

## 🚀 Core Architecture

The backend is built using **Spring Boot 3.2.5** and follows a modular service-oriented architecture. It utilizes a dual-database strategy:
- **MongoDB**: Stores high-volume, semi-structured data like social signals, trends, and enriched product details.
- **PostgreSQL**: Stores relational data including user profiles, authentication, and engagement metrics (click events).

### Tech Stack
- **Language**: Java 17
- **Framework**: Spring Boot 3
- **AI**: Groq (LLaMA 3.3 70b) via Spring AI
- **Automation**: Playwright (Headless Browser) for web scraping
- **Databases**: MongoDB & PostgreSQL
- **Security**: Spring Security + JWT
- **External APIs**: Reddit API, YouTube Data API v3

---

## 🛠 The Trend Engine (Data Pipeline)

The heart of TrendZY is its automated three-stage data pipeline, orchestrated by the `PipelineOrchestratorService`.

### 1. Signal Collection (`RedditCollectorService` & `YouTubeCollectorService`)
- **Reddit**: Scrapes 25+ Gen-Z focused subreddits (e.g., `r/IndianFashion`, `r/SkincareAddiction`). It uses the `.json` endpoints to fetch new and hot posts.
- **YouTube**: Tracks trending videos and comments to identify emerging product mentions.
- **Filtering**: Signals are assigned a **Priority Score (1-3)** based on buy-intent keywords (e.g., "where to buy", "link please") and product-specific terms.
- **Noise Reduction**: Obvious non-fashion/non-consumer content (politics, crypto, etc.) is filtered out before AI processing to save tokens.

### 2. AI Analysis (`AiAnalysisService`)
- **LLM Integration**: Batches of signals are sent to **Groq (LLaMA 3.3 70b)**.
- **Extraction**: The AI identifies specific, buyable products mentioned in the signals.
- **Output**: Returns structured JSON containing:
  - `productName`, `category`, `subcategory`
  - `trendScore` (0-100) and `velocity` (growth %)
  - `aiSummary`: Why it's trending.
  - `indiaRelevanceNote`: Specific context for the Indian market.
- **Deduplication**: Uses case-insensitive and normalized name matching to ensure trends aren't duplicated.

### 3. Product Enrichment (`ProductEnrichmentService`)
- **Discovery**: Takes "Trends" from the AI and attempts to find the actual product on e-commerce platforms.
- **Platform Scrapers**: Uses **Playwright** to search:
  - **Amazon.in**
  - **Flipkart.com**
  - **Myntra.com** (with HTTP/2 disabled to bypass bot detection)
  - **Meesho.com**
- **Scoring Engine**: The `ProductScoringService` evaluates search results against a "Product Fingerprint" (brand, type, color).
  - Matches are awarded points for Brand (+25), Type (+20), and Color (+10).
  - "Combo/Pack" items are penalized to focus on individual trending pieces.
- **Final Output**: Enriches the trend with a `primaryImageUrl`, `shopUrl`, and current `price`.

---

## 📂 Project Structure

```text
com.trendzy
├── config          # Security, Mongo, JWT, and AI configurations
├── controller      # REST Endpoints (Trend, Product, User, etc.)
├── dto             # Data Transfer Objects for API requests/responses
├── model           # Entity definitions (JPA for Postgres, Mongo for Trends)
├── repository      # Data access layer (Spring Data JPA & MongoDB)
├── scraper         # Dedicated scraping logic for websites/Instagram
├── security        # JWT filtering and authentication logic
└── service         # Core business logic and Pipeline Orchestration
```

---

## 📡 API Endpoints

### 📊 Trends & Products
- `GET /api/trends`: Fetch all trends (supports `category`, `tier`, `search`).
- `GET /api/trends/{id}`: Get trend details.
- `GET /api/products/trend/{trendId}`: Get the enriched product details for a trend.
- `GET /api/trends/stats`: Get dashboard statistics (total trends, active signals).

### 🔐 Authentication
- `POST /api/auth/register`: Create a new user.
- `POST /api/auth/login`: Authenticate and receive a JWT.

### 🛒 Affiliate & Engagement
- `POST /api/affiliate/click`: Track when a user clicks a "Shop Now" link.
- `GET /api/curated`: Fetch editor-curated product collections.

---

## ⚙️ Configuration & Schedulers

TrendZY runs on a scheduled heartbeat (configurable in `application.yml`):
- **Collection**: Runs daily at 2 AM (fetches overnight social activity).
- **Analysis**: Runs daily at 4 AM (processes signals after collection).
- **Enrichment**: Runs daily at 6 AM (enriches new trends with product links).

---

## 🇮🇳 India-Specific Optimizations
- **Subreddit Selection**: Focuses on Indian regional subreddits for high signal density.
- **Currency**: All pricing is handled in **INR (₹)**.
- **Platform Priority**: Myntra is prioritized for Fashion trends due to its Gen-Z relevance, while Amazon/Flipkart are used for broader coverage.
- **Bot Mitigation**: Uses advanced Playwright configurations (User-Agent spoofing, stealth headers, HTTP/2 disabling) to ensure reliable data from Indian e-commerce sites.
