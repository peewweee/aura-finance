const state = {
    extractedTransactions: []
};

const extractForm = document.getElementById("extract-form");
const rawTextInput = document.getElementById("raw-text");
const extractStatus = document.getElementById("extract-status");
const extractedList = document.getElementById("extracted-list");
const confirmSelectedBtn = document.getElementById("confirm-selected-btn");

const transactionsStatus = document.getElementById("transactions-status");
const transactionsList = document.getElementById("transactions-list");

const analysisForm = document.getElementById("analysis-form");
const analysisStatus = document.getElementById("analysis-status");
const analysisResult = document.getElementById("analysis-result");

const simulationForm = document.getElementById("simulation-form");
const simulationStatus = document.getElementById("simulation-status");
const simulationResult = document.getElementById("simulation-result");

const strategyForm = document.getElementById("strategy-form");
const strategyStatus = document.getElementById("strategy-status");
const strategyResult = document.getElementById("strategy-result");
const statusAnimations = new WeakMap();
document.getElementById("refresh-transactions-inline-btn").addEventListener("click", loadTransactions);

extractForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    setStatus(extractStatus, "Extracting transactions...");
    try {
        const response = await postJson("/transaction-extraction", {
            rawText: rawTextInput.value.trim(),
            referenceDate: todayString()
        });

        state.extractedTransactions = response.map((transaction) => ({
            ...transaction,
            selected: true
        }));

        renderExtractedTransactions();
        setStatus(extractStatus, `Aura found ${response.length} possible transaction(s).`, "success");
    } catch (error) {
        state.extractedTransactions = [];
        renderExtractionWarning(error);
        setStatus(extractStatus, userFacingErrorMessage(error.message), "error");
    }
});

confirmSelectedBtn.addEventListener("click", async () => {
    const selected = state.extractedTransactions
        .filter((transaction) => transaction.selected)
        .map(stripSelectionFlag);

    if (selected.length === 0) {
        setStatus(extractStatus, "Select at least one extracted transaction before saving.", "error");
        return;
    }

    setStatus(extractStatus, "Saving confirmed transactions...");
    try {
        await postJson("/transaction-extraction/confirm", { transactions: selected });
        state.extractedTransactions = [];
        renderExtractedTransactions();
        setStatus(extractStatus, "Your selected entries were saved.", "success");
        await loadTransactions();
    } catch (error) {
        setStatus(extractStatus, error.message, "error");
    }
});

analysisForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    setStatus(analysisStatus, "Generating spending analysis...");
    try {
        const result = await postJson("/spending-analysis", {
            startDate: document.getElementById("analysis-start").value,
            endDate: document.getElementById("analysis-end").value
        });
        renderAnalysis(result);
        setStatus(analysisStatus, "Spending analysis ready.", "success");
    } catch (error) {
        renderAiWarning(analysisResult, error.message, "Aura needs a quick reset");
        setStatus(analysisStatus, userFacingErrorMessage(error.message), "error");
    }
});

simulationForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    setStatus(simulationStatus, "Calculating opportunity cost...");
    try {
        const result = await postJson("/purchase-simulation", {
            purchaseAmount: numberValue("simulation-amount"),
            expectedMonthlyReturnRate: numberValue("simulation-rate"),
            timeHorizonMonths: integerValue("simulation-months")
        });
        renderSimulation(result);
        setStatus(simulationStatus, "Simulation ready.", "success");
    } catch (error) {
        setStatus(simulationStatus, error.message, "error");
    }
});

strategyForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    setStatus(strategyStatus, "Generating strategy guidance...");
    try {
        const body = {
            startDate: document.getElementById("strategy-start").value,
            endDate: document.getElementById("strategy-end").value
        };

        const amount = document.getElementById("strategy-amount").value;
        const rate = document.getElementById("strategy-rate").value;
        const months = document.getElementById("strategy-months").value;

        if (amount) {
            body.plannedPurchaseAmount = Number(amount);
        }
        if (rate) {
            body.expectedMonthlyReturnRate = Number(rate);
        }
        if (months) {
            body.timeHorizonMonths = Number(months);
        }

        const result = await postJson("/financial-strategy/explain", body);
        renderStrategy(result);
        setStatus(strategyStatus, "Financial strategy guidance ready.", "success");
    } catch (error) {
        renderAiWarning(strategyResult, error.message, "Aura is resting for a bit");
        setStatus(strategyStatus, userFacingErrorMessage(error.message), "error");
    }
});

function renderExtractedTransactions() {
    if (state.extractedTransactions.length === 0) {
        extractedList.innerHTML = "No extracted transactions yet.";
        extractedList.className = "draft-list empty-state";
        confirmSelectedBtn.disabled = true;
        return;
    }

    extractedList.className = "draft-list";
    extractedList.innerHTML = `<div class="draft-grid">${
        state.extractedTransactions.map((transaction, index) => `
            <div class="draft-item">
                <input class="draft-check" type="checkbox" ${transaction.selected ? "checked" : ""} data-action="toggle" data-index="${index}">
                <label class="field">
                    <span>Description</span>
                    <input type="text" value="${escapeHtml(transaction.description)}" data-action="description" data-index="${index}">
                </label>
                <label class="field">
                    <span>Amount</span>
                    <input type="number" step="0.01" min="0.01" value="${transaction.amount}" data-action="amount" data-index="${index}">
                </label>
                <label class="field">
                    <span>Category</span>
                    <input type="text" value="${escapeHtml(transaction.category)}" data-action="category" data-index="${index}">
                </label>
                <label class="field">
                    <span>Date</span>
                    <input type="date" value="${transaction.transactionDate}" data-action="date" data-index="${index}">
                </label>
            </div>
        `).join("")
    }</div>`;

    extractedList.querySelectorAll("input").forEach((input) => {
        input.addEventListener("input", handleDraftEdit);
        input.addEventListener("change", handleDraftEdit);
    });

    confirmSelectedBtn.disabled = state.extractedTransactions.every((transaction) => !transaction.selected);
}

function handleDraftEdit(event) {
    const index = Number(event.target.dataset.index);
    const action = event.target.dataset.action;
    const draft = state.extractedTransactions[index];

    if (!draft) {
        return;
    }

    if (action === "toggle") {
        draft.selected = event.target.checked;
    } else if (action === "description") {
        draft.description = event.target.value;
    } else if (action === "amount") {
        draft.amount = Number(event.target.value);
    } else if (action === "category") {
        draft.category = event.target.value;
    } else if (action === "date") {
        draft.transactionDate = event.target.value;
    }

    confirmSelectedBtn.disabled = state.extractedTransactions.every((transaction) => !transaction.selected);
}

async function loadTransactions() {
    setStatus(transactionsStatus, "Loading saved transactions...");
    try {
        const transactions = await fetchJson("/transactions");

        if (transactions.length === 0) {
            transactionsList.className = "table-shell empty-state";
            transactionsList.textContent = "No saved transactions yet.";
        } else {
            transactionsList.className = "table-shell";
            transactionsList.innerHTML = `
                <table class="table">
                    <thead>
                        <tr>
                            <th>Description</th>
                            <th>Amount</th>
                            <th>Category</th>
                            <th>Date</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${transactions.map((transaction) => `
                            <tr>
                                <td>${escapeHtml(transaction.description)}</td>
                                <td>${formatCurrency(transaction.amount)}</td>
                                <td><span class="pill">${escapeHtml(transaction.category)}</span></td>
                                <td>${escapeHtml(transaction.transactionDate)}</td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            `;
        }

        setStatus(transactionsStatus, `Loaded ${transactions.length} saved transaction(s).`, "success");
    } catch (error) {
        setStatus(transactionsStatus, error.message, "error");
    }
}

function renderAnalysis(result) {
    const categoryRows = Object.entries(result.spendingByCategory || {})
        .map(([category, amount]) => `<li><strong>${escapeHtml(category)}:</strong> ${formatCurrency(amount)}</li>`)
        .join("");

    analysisResult.className = "result-card";
    analysisResult.innerHTML = `
        <div class="metrics">
            <div class="metric">
                <span class="metric-label">Transactions</span>
                <span class="metric-value">${result.transactionCount}</span>
            </div>
            <div class="metric">
                <span class="metric-label">Total Spent</span>
                <span class="metric-value">${formatCurrency(result.totalSpent)}</span>
            </div>
            <div class="metric">
                <span class="metric-label">Date Range</span>
                <span class="metric-value">${result.startDate} to ${result.endDate}</span>
            </div>
        </div>
        <h3>Summary</h3>
        <p>${formatAdviceText(result.summary)}</p>
        <h4>Insights</h4>
        <ul class="list-block">${result.insights.map((item) => `<li>${formatAdviceText(item)}</li>`).join("")}</ul>
        <h4>Recommendations</h4>
        <ul class="list-block">${result.recommendations.map((item) => `<li>${formatAdviceText(item)}</li>`).join("")}</ul>
        <h4>Category Breakdown</h4>
        <ul class="list-block">${categoryRows || "<li>No category totals available.</li>"}</ul>
    `;
}

function renderSimulation(result) {
    simulationResult.className = "result-card";
    simulationResult.innerHTML = `
        <div class="metrics">
            <div class="metric">
                <span class="metric-label">Spending Now</span>
                <span class="metric-value">${formatCurrency(result.purchaseAmount)}</span>
            </div>
            <div class="metric">
                <span class="metric-label">Future Savings</span>
                <span class="metric-value">${formatCurrency(result.futureValueIfInvested)}</span>
            </div>
            <div class="metric">
                <span class="metric-label">Money You Give Up</span>
                <span class="metric-value">${formatCurrency(result.opportunityCost)}</span>
            </div>
        </div>
        <p>${escapeHtml(result.explanation)}</p>
    `;
}

function renderStrategy(result) {
    const categoryRows = Object.entries(result.spendingByCategory || {})
        .map(([category, amount]) => `<li><strong>${escapeHtml(category)}:</strong> ${formatCurrency(amount)}</li>`)
        .join("");

    strategyResult.className = "result-card";
    strategyResult.innerHTML = `
        <div class="metrics">
            <div class="metric">
                <span class="metric-label">Entries</span>
                <span class="metric-value">${result.transactionCount}</span>
            </div>
            <div class="metric">
                <span class="metric-label">Spent In Range</span>
                <span class="metric-value">${formatCurrency(result.totalSpent)}</span>
            </div>
            <div class="metric">
                <span class="metric-label">Money You Give Up</span>
                <span class="metric-value">${result.opportunityCost ? formatCurrency(result.opportunityCost) : "N/A"}</span>
            </div>
        </div>
        <h3>Spending Insight</h3>
        <p>${formatAdviceText(result.spendingInsight)}</p>
        <h4>Caution Flags</h4>
        <ul class="list-block">${result.cautionFlags.map((item) => `<li>${formatAdviceText(item)}</li>`).join("")}</ul>
        <h4>Recommendation</h4>
        <p>${formatAdviceText(result.recommendationText)}</p>
        <h4>Category Breakdown</h4>
        <ul class="list-block">${categoryRows || "<li>No category totals available.</li>"}</ul>
    `;
}

function renderExtractionWarning(error) {
    extractedList.className = "draft-list";
    extractedList.innerHTML = buildWarningCardHtml(
        "Aura is resting for a bit",
        userFacingErrorDetail(error.message, "Try again in a little while. If several people are testing the app, Aura may have hit its AI usage limit for now.")
    );
    confirmSelectedBtn.disabled = true;
}

function renderAiWarning(target, message, title) {
    target.className = "result-card";
    target.innerHTML = buildWarningCardHtml(
        title,
        userFacingErrorDetail(message, "Please try again shortly. If the app is busy, the AI quota may need a little time to refresh.")
    );
}

function buildWarningCardHtml(title, message) {
    return `
        <div class="warning-card">
            <span class="warning-badge">Aura Notice</span>
            <h3 class="warning-title">${escapeHtml(title)}</h3>
            <p class="warning-copy">${escapeHtml(message)}</p>
        </div>
    `;
}

async function fetchJson(url) {
    const response = await fetch(url);
    return handleResponse(response);
}

async function postJson(url, body) {
    const response = await fetch(url, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body)
    });
    return handleResponse(response);
}

async function handleResponse(response) {
    const text = await response.text();
    const data = text ? safeJsonParse(text) : null;

    if (!response.ok) {
        throw new Error(extractErrorMessage(data) || `Request failed with status ${response.status}.`);
    }

    return data;
}

function extractErrorMessage(data) {
    if (!data) {
        return null;
    }
    if (typeof data.message === "string") {
        return data.message;
    }
    if (Array.isArray(data.errors) && data.errors.length > 0) {
        return data.errors.map((error) => `${error.field}: ${error.error}`).join(" | ");
    }
    return null;
}

function safeJsonParse(text) {
    try {
        return JSON.parse(text);
    } catch {
        return { message: text };
    }
}

function setStatus(element, message, type = "") {
    stopStatusAnimation(element);
    element.className = `status ${type}`.trim();

    if (!type && typeof message === "string" && message.endsWith("...")) {
        startStatusAnimation(element, message.slice(0, -3));
        return;
    }

    element.textContent = message;
}

function userFacingErrorMessage(message) {
    if (isQuotaOrRateLimitError(message)) {
        return "Aura is temporarily busy right now. Please try again shortly.";
    }
    return message;
}

function userFacingErrorDetail(message, fallback) {
    if (isQuotaOrRateLimitError(message)) {
        return fallback;
    }
    return message || fallback;
}

function isQuotaOrRateLimitError(message) {
    const normalized = String(message ?? "").toLowerCase();
    return normalized.includes("quota")
        || normalized.includes("resource_exhausted")
        || normalized.includes("rate limit")
        || normalized.includes("too many requests")
        || normalized.includes("429")
        || normalized.includes("exceeded");
}

function startStatusAnimation(element, baseMessage) {
    const dots = ["", ".", "..", "..."];
    let frame = 0;

    element.textContent = `${baseMessage}${dots[dots.length - 1]}`;

    const intervalId = setInterval(() => {
        frame = (frame + 1) % dots.length;
        element.textContent = `${baseMessage}${dots[frame]}`;
    }, 350);

    statusAnimations.set(element, intervalId);
}

function stopStatusAnimation(element) {
    const intervalId = statusAnimations.get(element);
    if (intervalId) {
        clearInterval(intervalId);
        statusAnimations.delete(element);
    }
}

function stripSelectionFlag(transaction) {
    return {
        description: transaction.description,
        amount: Number(transaction.amount),
        category: transaction.category,
        transactionDate: transaction.transactionDate
    };
}

function formatCurrency(value) {
    return new Intl.NumberFormat("en-PH", {
        style: "currency",
        currency: "PHP"
    }).format(Number(value));
}

function formatAdviceText(value) {
    return escapeHtml(normalizeMoneyText(value));
}

function normalizeMoneyText(value) {
    const text = String(value ?? "");
    return text
        .replace(/([₱])\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.\d{2})?|[0-9]{4,}(?:\.\d{2})?)/g, (_, symbol, amount) => `${symbol}${formatPlainAmount(amount)}`)
        .replace(/\b(PHP|pesos?)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.\d{2})?|[0-9]{4,}(?:\.\d{2})?)/gi, (_, label, amount) => `${label} ${formatPlainAmount(amount)}`)
        .replace(/\b([0-9]{4,}\.\d{2})\b/g, (_, amount) => formatPlainAmount(amount));
}

function formatPlainAmount(value) {
    const numeric = Number(String(value).replaceAll(",", ""));
    if (!Number.isFinite(numeric)) {
        return value;
    }

    return new Intl.NumberFormat("en-PH", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }).format(numeric);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#39;");
}

function todayString() {
    return new Date().toISOString().slice(0, 10);
}

function numberValue(id) {
    return Number(document.getElementById(id).value);
}

function integerValue(id) {
    return Number.parseInt(document.getElementById(id).value, 10);
}

function setDefaultDates() {
    const now = new Date();
    const start = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().slice(0, 10);
    const end = now.toISOString().slice(0, 10);
    document.getElementById("analysis-start").value = start;
    document.getElementById("analysis-end").value = end;
    document.getElementById("strategy-start").value = start;
    document.getElementById("strategy-end").value = end;
}

setDefaultDates();
renderExtractedTransactions();
loadTransactions();
