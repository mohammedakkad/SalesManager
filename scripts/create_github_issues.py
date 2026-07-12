#!/usr/bin/env python3
"""Create Phase 1-3 GitHub issues for SalesManager roadmap."""

import json
import subprocess
import sys
import urllib.error
import urllib.request

REPO = "mohammedakkad/SalesManager"


def get_token() -> str:
    url = subprocess.check_output(
        ["git", "remote", "get-url", "origin"], text=True
    ).strip()
    return url.split("x-access-token:")[1].split("@")[0]


def api_request(method: str, path: str, data: dict | None = None) -> dict:
    token = get_token()
    body = json.dumps(data).encode() if data is not None else None
    req = urllib.request.Request(
        f"https://api.github.com{path}",
        data=body,
        method=method,
        headers={
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "Content-Type": "application/json",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        err = e.read().decode()
        raise RuntimeError(f"{method} {path} failed ({e.code}): {err}") from e


def ensure_label(name: str, color: str, description: str) -> None:
    try:
        api_request("POST", f"/repos/{REPO}/labels", {
            "name": name,
            "color": color,
            "description": description,
        })
        print(f"  + label: {name}")
    except RuntimeError as e:
        if "already_exists" in str(e) or "422" in str(e):
            print(f"  = label exists: {name}")
        else:
            raise


def create_issue(title: str, body: str, labels: list[str]) -> dict:
    return api_request("POST", f"/repos/{REPO}/issues", {
        "title": title,
        "body": body,
        "labels": labels,
    })


ISSUES = [
    {
        "ref": "28",
        "title": "Real Profit Calculation (Cost of Goods Sold accuracy)",
        "labels": ["enhancement", "P0", "phase-1"],
        "priority": "P0",
        "effort": "Medium",
        "why": """Product.kt already has `costPrice`, `profitMargin`, `profitPercent`, and `ReportsRepositoryImpl.observeDailySalesProfit` exists — but `DashboardAnalytics.kt` currently has **no profit field at all** (only `todaySummary`, `totalOutstandingDebt`, `topSellingProducts`, `topDebtorCustomers`, `lastSevenDaysSales`). The merchant sees revenue everywhere but never real profit at a glance. This is table-stakes for any accounting-adjacent app and currently a blind spot.""",
        "scope": """- Add `todayProfit: Double` and `monthProfit: Double` to `DashboardAnalytics`, computed from `InvoiceItem.quantity × (unitPrice - product.costPrice)` at time of sale — **NOT** live product cost, to avoid retroactively changing historical profit when a merchant updates a cost price later. This likely means `InvoiceItem` needs to snapshot `costPrice` at sale time if it doesn't already — verify and add if missing.
- Surface profit clearly in `AnalyticsDashboard.kt` alongside sales (not hidden inside Reports).
- Handle products with `costPrice = 0` gracefully: exclude from profit calc or flag as "غير محدد" rather than showing a false 100% margin.
- Add a profit column/line to the single-invoice PDF for internal/owner use only — never on the customer-facing copy (this is sensitive business data).""",
        "acceptance": """- Dashboard and Reports show real profit (revenue − COGS), not just revenue, and match each other's numbers exactly for the same period.
- Editing a product's cost price today does not silently change last month's reported profit.""",
    },
    {
        "ref": "29",
        "title": "Dashboard Enhancement (Phase 2 of Analytics Dashboard)",
        "labels": ["enhancement", "P0", "phase-1"],
        "priority": "P0",
        "effort": "Medium",
        "why": """The dashboard shipped with sales totals, top products, top debtors, and a 7-day chart — solid foundation, but missing profit (see #28) and a few other signals merchants expect from a daily "business health" view, per the competitive gap review against Loyverse/Daftra-style dashboards.""",
        "scope": """- Integrate the new profit metric from #28 into the Top Indicators row.
- Add a "low stock" indicator card (count of products below threshold — ties into #30, reuse the same query rather than duplicating logic).
- Add a period selector (اليوم / هذا الأسبوع / هذا الشهر) to the dashboard instead of hardcoded "today" + "last 7 days," reusing `GetDashboardAnalyticsUseCase` with a date-range parameter.
- Performance check: confirm no redundant Room queries were introduced (this was flagged as a hard constraint in the original dashboard implementation).""",
        "acceptance": """- Merchant can see today/week/month toggled from one dashboard without navigating to Reports.
- Profit and low-stock indicators are present and accurate.""",
        "blocked_by": ["#28", "#30"],
    },
    {
        "ref": "30",
        "title": "Automatic Low-Stock Alerts",
        "labels": ["enhancement", "P0", "phase-1"],
        "priority": "P0",
        "effort": "Low",
        "why": """`StockMovementDao`/`StockRepositoryImpl` already track inventory movement, but there is no proactive notification when a product's quantity drops below a safe threshold — the merchant has to remember to check manually. This is a standard feature across every competing POS (Loyverse, Cash Top) and currently a real gap.""",
        "scope": """- Add `lowStockThreshold: Int` to `Product` (nullable/optional, default null = alerts disabled for that product, merchant opts in per product or sets a global default in Settings).
- New `CoroutineWorker` (e.g., `LowStockCheckWorker`), modeled exactly on the existing `UnpaidDebtWorker`/`DebtReminderCheckWorker` pattern already in the codebase — daily periodic check, single summary notification ("لديك X منتجات أوشكت على النفاد") deep-linking into a filtered Inventory list.
- Add a visual low-stock badge/indicator on `InventoryListScreen` items already below threshold, independent of the notification (immediate visibility when browsing, not just via notification).""",
        "acceptance": """- Merchant gets a daily notification when any product crosses below its threshold.
- Products under threshold are visually flagged in the inventory list at all times, not just via notification.""",
    },
    {
        "ref": "31",
        "title": "Sale Screen (POS Checkout Flow) Improvements",
        "labels": ["enhancement", "P0", "phase-1"],
        "priority": "P0",
        "effort": "Medium-High",
        "why": """`AddEditTransactionScreen.kt` is the highest-frequency screen in the app (used per sale, multiple times a day) — small friction here compounds massively across a merchant's day. Competing POS apps (Loyverse in particular) are explicitly reviewed as "fast checkout" as their #1 selling point.""",
        "scope": """- Reduce taps-to-complete-sale: audit current flow for unnecessary steps between opening the screen and confirming a cash sale.
- Quick-add via barcode scanner directly from this screen (reuse existing `BarcodeScannerScreen`) instead of only manual product search, if not already wired here.
- Numeric keypad optimized for quantity/price entry (large touch targets — this screen is often used quickly, sometimes one-handed).
- Auto-focus and smart defaults: default payment type based on last-used, auto-select "زبون زائر" unless a customer is searched.
- Visual running total that updates instantly as items are added (verify current recomposition performance here specifically — this screen must feel instant, zero lag, per the "best performance" competitive goal).""",
        "acceptance": """- Completing a 3-item cash sale takes measurably fewer taps/seconds than the current flow.
- No perceptible lag adding/removing items even with 20+ items in a single sale.""",
    },
    {
        "ref": "32",
        "title": "WhatsApp Invoice Sharing (elevate text-share to PDF-share)",
        "labels": ["enhancement", "P0", "phase-1"],
        "priority": "P0",
        "effort": "Low",
        "why": """`InvoicePdfGenerator.kt` already exists from the PDF export/print task; `InvoiceSharer.kt` currently only sends a plain text summary via WhatsApp. Closing this loop (PDF, not just text, sent directly to WhatsApp) is the single most visible "looks professional" moment for the customer receiving it.""",
        "scope": """- Wire `InvoicePdfGenerator`'s output file into `InvoiceSharer`'s WhatsApp intent (`type = "application/pdf"` instead of `"text/plain"`), reusing the existing `FileProvider` setup.
- Offer both options clearly in the UI: "مشاركة نصية سريعة" (existing) vs "مشاركة PDF احترافي" (new) — do not remove the fast text option, some merchants will prefer it for speed.
- One-tap "send to customer" directly from the Sale Screen (#31) right after completing a sale — not just from `TransactionDetailsScreen` after the fact, so it fits naturally into the checkout flow.""",
        "acceptance": """- Merchant can send a professional PDF invoice via WhatsApp within 1-2 taps immediately after completing a sale.""",
        "related": ["#31"],
    },
    {
        "ref": "33",
        "title": "Employee Permissions (Role-Based Access Control)",
        "labels": ["enhancement", "P1", "phase-2"],
        "priority": "P1",
        "effort": "Medium-High",
        "why": """`EmployeeManagementScreen`/`EmployeeRepositoryImpl` already exist, but permission granularity needs verification — competing apps (Loyverse) market role-based permissions as a core selling point for owners who don't want every employee able to edit prices, delete transactions, or view full financial reports.""",
        "scope": """- Define role tiers (e.g., مدير / بائع) with explicit permission flags: can view reports, can edit product prices, can delete transactions, can access Settings/Backup.
- Enforce these checks at the ViewModel/UseCase layer (not just hiding UI buttons) so permission checks can't be bypassed by direct navigation.
- Link each Transaction to the acting Employee (ties into the earlier "per-employee sales report" gap) if not already present.""",
        "acceptance": """- A restricted employee account cannot access or perform gated actions, even via deep links.
- Owner can generate a per-employee sales report.""",
    },
    {
        "ref": "34",
        "title": "Excel Import (Bulk Product/Customer Onboarding)",
        "labels": ["enhancement", "P1", "phase-2"],
        "priority": "P1",
        "effort": "Medium",
        "why": """`XlsxWriter.kt` already exists for export — the missing half is import, which matters enormously for onboarding: a merchant migrating from a paper ledger or another system needs to bulk-load products/customers rather than typing each one manually.""",
        "scope": """- Reuse existing xlsx dependency (already present for export) for reading.
- Product import: name, price, costPrice, quantity, unit — with a downloadable template and clear validation errors (row-level, in Arabic) rather than a silent failure.
- Customer import: name, phone.
- Duplicate detection (by name or existing identifier) with merchant confirmation before overwrite.""",
        "acceptance": """- Merchant can import 100+ products from a correctly formatted Excel file in under a minute, with clear per-row error feedback for malformed data.""",
    },
    {
        "ref": "35",
        "title": "Advanced Reports",
        "labels": ["enhancement", "P1", "phase-2"],
        "priority": "P1",
        "effort": "Medium",
        "why": """`ReportsRepositoryImpl` already covers financial totals, daily sales/profit, inventory value, debt aging — this task extends depth rather than building from scratch.""",
        "scope": """- Product-level profit report (not just top-5, full sortable list with margin %).
- Custom date-range comparison (this month vs last month).
- Category/tag-based breakdown if product categorization exists (verify Product model).""",
        "acceptance": """- Merchant can answer "which products are actually most profitable, not just best-selling" directly from Reports.""",
        "blocked_by": ["#28"],
    },
    {
        "ref": "36",
        "title": "Customer Segmentation/Classification",
        "labels": ["enhancement", "P1", "phase-2"],
        "priority": "P1",
        "effort": "Low-Medium",
        "why": """`Customer.kt` currently only has id/name/phone/createdAt/syncStatus — no way to tag VIP customers, frequent buyers, or at-risk debtors beyond the raw debt-aging report.""",
        "scope": """- Add optional customer tags/segments (e.g., VIP, عادي, متعثر) — simple enum or free-tag field, non-breaking Room migration.
- Surface segment in `CustomerDetailsScreen` and allow filtering `CustomersScreen` by segment.
- Optional: auto-suggest "متعثر" tag based on debt-aging data from Reports, but keep it merchant-confirmed, not fully automatic (avoid mislabeling).""",
        "acceptance": """- Merchant can filter/sort customers by segment and manually tag them.""",
    },
    {
        "ref": "37",
        "title": "Web Admin Dashboard",
        "labels": ["enhancement", "P2", "phase-3"],
        "priority": "P2",
        "effort": "High",
        "why": """Directly addresses the competitive gap where rivals advertise laptop/iPad access; Firebase RTDB already backing the app makes a read-first web dashboard realistic without duplicating business logic.""",
        "scope": """**High-level scope — deserves its own dedicated planning pass.**

- Start read-only: sales overview, reports, inventory value — mirrors the mobile Dashboard, not a full re-implementation of every screen.
- Firebase Auth reusing existing merchant accounts.
- Explicitly **NOT** a POS checkout flow on web for v1 — scope creep risk is high here.""",
        "acceptance": """- Merchant can check today's numbers from a browser without installing anything.""",
        "note": "Separate project, not an app feature. Ship only after Phase 1+2 are stable.",
    },
    {
        "ref": "39",
        "title": "Enterprise Analytics (multi-branch / larger business support)",
        "labels": ["enhancement", "P2", "phase-3", "research"],
        "priority": "P2",
        "effort": "Very High (architectural)",
        "why": """This is the single largest gap and the largest undertaking (`merchantId` is currently single-tenant-per-merchant by design) — explicitly deferred until there's validated demand, per earlier competitive analysis. **Do not start this speculatively.**""",
        "scope": """Requires a dedicated architecture/design session before any implementation:
- Multi-branch data model
- Cross-branch reporting
- Branch-level permissions""",
        "acceptance": """N/A until scoped separately — this issue should stay as a research/discussion item, not an implementation-ready ticket, until Phase 1+2 are shipped and real merchant demand is confirmed.""",
        "note": "Research/discussion item only — not implementation-ready.",
    },
]


def build_body(issue: dict) -> str:
    lines = [
        f"## Roadmap Task #{issue['ref']}",
        "",
        f"| Field | Value |",
        f"|-------|-------|",
        f"| **Priority** | {issue['priority']} |",
        f"| **Effort** | {issue['effort']} |",
        "",
        "## Why",
        issue["why"],
        "",
        "## Scope",
        issue["scope"],
        "",
        "## Acceptance Criteria",
        issue["acceptance"],
    ]
    if issue.get("blocked_by"):
        lines += ["", "## Blocked by", ", ".join(issue["blocked_by"])]
    if issue.get("related"):
        lines += ["", "## Related", ", ".join(issue["related"])]
    if issue.get("note"):
        lines += ["", "## Note", issue["note"]]
    return "\n".join(lines)


def main() -> int:
    created = []
    for issue in ISSUES:
        title = f"[#{issue['ref']}] {issue['title']}"
        body = build_body(issue)
        # Use only default labels the token can assign
        labels = [l for l in issue["labels"] if l == "enhancement"]
        print(f"Creating: {title}")
        result = create_issue(title, body, labels)
        created.append((result["number"], title, result["html_url"]))
        print(f"  -> #{result['number']} {result['html_url']}")

    print("\n=== Created Issues ===")
    for num, title, url in created:
        print(f"#{num}: {title}")
        print(f"     {url}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
