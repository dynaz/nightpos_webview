/** @odoo-module **/

/**
 * Build plain-text receipt content for NightPOS TCP / Sunmi printers.
 * @param {import("@point_of_sale/app/models/pos_order").PosOrder} order
 * @param {object} config  pos.config record (frontend model)
 * @param {{ isReprint?: boolean }} options
 */
export function formatReceiptText(order, config, { isReprint = false } = {}) {
    const paper = config.nightpos_sunmi_paper || "58";
    const colW = paper === "80" ? 48 : 32;
    const priceW = 10;
    const nameW = colW - priceW;
    const sep = (paper === "80" ? "─".repeat(48) : "─".repeat(32)) + "\n";

    const fmt = (n) =>
        Number(n).toLocaleString("th-TH", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
        });
    const padEnd = (str, len) => String(str).padEnd(len, " ").substring(0, len);
    const padStart = (str, len) => String(str).padStart(len, " ").substring(0, len);

    const shopName = config.company_id?.name || "";
    const name = order.name || order.pos_reference || "";
    const date = order.date_order?.toFormat?.("yyyy-MM-dd HH:mm") || String(order.date_order || "");
    const employee = order.user_id?.name || "";
    const lines = order.getOrderlines();
    const total = order.priceIncl ?? 0;
    const paid = order.amountPaid ?? 0;
    const change = order.change ?? 0;

    const linesText = [];
    if (shopName) {
        linesText.push(`${shopName}\n`);
    }
    if (isReprint) {
        linesText.push("** REPRINT **\n");
    }
    linesText.push(`Receipt: ${name}\n`);
    if (date) {
        linesText.push(`Date   : ${date}\n`);
    }
    if (employee) {
        linesText.push(`Cashier: ${employee}\n`);
    }
    linesText.push(sep);
    linesText.push(`${padEnd("Item", nameW)}${padStart("Total", priceW)}\n`);
    linesText.push(sep);

    for (const line of lines) {
        const lineName = line.full_product_name || line.product_id?.display_name || "";
        const lineQty = line.qty ?? 1;
        const linePrice = line.prices?.total_included ?? 0;
        const unitPrice = line.prices?.base_line_total_excluded
            ? linePrice / lineQty
            : linePrice / lineQty;
        linesText.push(`${lineName}\n`);
        const qtyStr = `  ${fmt(lineQty)} × ${fmt(unitPrice)}`;
        linesText.push(`${padEnd(qtyStr, nameW)}${padStart(fmt(linePrice), priceW)}\n`);
    }

    linesText.push(sep);
    linesText.push(`${padEnd("TOTAL", nameW)}${padStart(fmt(total), priceW)}\n`);
    linesText.push(`${padEnd("PAID", nameW)}${padStart(fmt(paid), priceW)}\n`);
    if (change > 0) {
        linesText.push(`${padEnd("CHANGE", nameW)}${padStart(fmt(change), priceW)}\n`);
    }

    return linesText.join("");
}
