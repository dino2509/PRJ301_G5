package com.smartshop.servlet.admin;

import com.smartshop.dao.ProductPricingDAO;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;

public class ProductPricingAdminServlet extends HttpServlet {

    private String nvl(String s) { return s == null ? "" : s.trim(); }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String action = nvl(req.getParameter("action")); // update | clear
        String pidStr = nvl(req.getParameter("productId"));
        String saleStr = nvl(req.getParameter("sale")); // %
        String salePriceStr = nvl(req.getParameter("sale_price")); // VND
        String back = nvl(req.getParameter("return")); // URL để quay lại

        if (!pidStr.matches("\\d+")) {
            resp.sendError(400, "productId không hợp lệ");
            return;
        }
        int productId = Integer.parseInt(pidStr);

        ProductPricingDAO dao = new ProductPricingDAO();
        try {
            if ("clear".equalsIgnoreCase(action)) {
                dao.clearSale(productId);
                redirect(resp, back, req.getContextPath() + "/admin/products?ok=cleared");
                return;
            }

            if (!"update".equalsIgnoreCase(action)) {
                resp.sendError(400, "action không hợp lệ");
                return;
            }

            boolean hasSale = !saleStr.isEmpty();
            boolean hasSalePrice = !salePriceStr.isEmpty();
            if (!hasSale && !hasSalePrice) {
                redirect(resp, back, req.getContextPath() + "/admin/products?err=empty");
                return;
            }
            if (hasSale && hasSalePrice) {
                // Ưu tiên sale_price nếu gửi cả hai
                hasSale = false;
            }

            if (hasSale) {
                BigDecimal sale = new BigDecimal(saleStr);
                if (sale.compareTo(BigDecimal.ZERO) < 0 || sale.compareTo(new BigDecimal("100")) > 0) {
                    redirect(resp, back, req.getContextPath() + "/admin/products?err=sale_range");
                    return;
                }
                dao.updateSalePercent(productId, sale);
            } else {
                BigDecimal sp = new BigDecimal(salePriceStr);
                if (sp.compareTo(BigDecimal.ZERO) < 0) {
                    redirect(resp, back, req.getContextPath() + "/admin/products?err=sp_neg");
                    return;
                }
                dao.updateSalePrice(productId, sp);
            }
            redirect(resp, back, req.getContextPath() + "/admin/products?ok=updated");
        } catch (NumberFormatException e) {
            resp.sendError(400, "Định dạng số không hợp lệ");
        } catch (SQLException e) {
            throw new ServletException(e);
        }
    }

    private void redirect(HttpServletResponse resp, String back, String fallback) throws IOException {
        if (!back.isEmpty()) {
            resp.sendRedirect(back);
        } else {
            resp.sendRedirect(fallback);
        }
    }
}
