package com.example.datn.service;

import com.example.datn.config.CloudinaryService;
import com.example.datn.dto.NhanVienDTO;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.VaiTroRepository;
import com.example.datn.vo.nhanVienVO.NhanVienQueryVO;
import com.example.datn.vo.nhanVienVO.NhanVienUpdateVO;
import com.example.datn.vo.nhanVienVO.NhanVienVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.sql.Date;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class NhanVienService {
    private static final Logger log = LoggerFactory.getLogger(NhanVienService.class);

    @Autowired
    private VaiTroRepository vaiTroRepository;

    private final NhanVienRepository nhanVienRepository;

    @Autowired
    @Qualifier("emailConfigService")
    private com.example.datn.config.EmailService emailConfigService;

    @Autowired
    private CloudinaryService cloudinaryService;

    @Autowired
    public NhanVienService(NhanVienRepository nhanVienRepository) {
        this.nhanVienRepository = nhanVienRepository;
    }

    @Transactional
    public Integer save(NhanVienVO vO, MultipartFile imageFile) {
        NhanVien bean = new NhanVien();
        BeanUtils.copyProperties(vO, bean);

        // Lưu ảnh lên Cloudinary nếu có file upload
        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = cloudinaryService.uploadImage(imageFile);
                bean.setHinhAnh(imageUrl);
            } catch (Exception e) {
                // Sử dụng logger
                log.error("Không thể upload ảnh nhân viên lên Cloudinary: {}", e.getMessage(), e);
                throw new RuntimeException("Không thể upload ảnh lên Cloudinary: " + e.getMessage());
            }
        }

        // THAY ĐỔI 2: Cải thiện xử lý lỗi khi không tìm thấy VaiTro
        if (vO.getIdVaiTro() != null) {
            VaiTro vaiTro = vaiTroRepository.findById(vO.getIdVaiTro())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy VaiTro với ID: " + vO.getIdVaiTro()));
            bean.setVaiTro(vaiTro);
        } else {
            bean.setVaiTro(null);
        }

        bean = nhanVienRepository.save(bean);

        // ---- PHẦN GỬI EMAIL GIỮ NGUYÊN LOGIC NHƯNG CẢI THIỆN LOGGING ----
        if (emailConfigService != null && bean.getEmail() != null && !bean.getEmail().trim().isEmpty()) {
            String subject = "🎉 Tài khoản nhân viên đã được tạo thành công! 🎉";
            // Nội dung email được tạo trực tiếp tại đây theo yêu cầu
            String body = "<div style=\"font-family:'Segoe UI',Arial,sans-serif;background:#f9fafd;padding:32px 0;\">"
                    + "<div style=\"max-width:520px;margin:0 auto;background:#fff;border-radius:16px;box-shadow:0 4px 24px #e3e3ec;padding:40px 32px 32px 32px;\">"
                    + "<div style=\"text-align:center;\">"
                    + "    <img src=\"https://i.imgur.com/3fJ1P48.png\" alt=\"Logo Shop\" style=\"width:80px;margin-bottom:16px;\">"
                    + "    <h2 style=\"color:#1976d2;margin-bottom:8px;letter-spacing:1px;\">Chào mừng bạn gia nhập Menz Fashion Shirt Shop!</h2>"
                    + "    <p style=\"color:#444;font-size:17px;margin:0 0 20px 0;\">Xin chào <b style='color:#1976d2'>" + bean.getHoVaTen() + "</b>,</p>"
                    + "</div>"
                    + "<div style=\"background:#f7fbfd;border-radius:12px;padding:24px 18px;margin:18px 0 22px 0;border:1.5px solid #e3f3fc;\">"
                    + "    <div style=\"font-size:17px;\">"
                    + "        <span style=\"color:#1976d2;font-weight:600;\">Thông tin đăng nhập của bạn:</span><br>"
                    + "        <table style=\"width:100%;margin-top:12px;font-size:16px;\">"
                    + "            <tr><td style=\"padding:6px 0;color:#888;\">Tên đăng nhập:</td><td style=\"font-weight:700;color:#1976d2;\">" + bean.getEmail() + "</td></tr>"
                    + "            <tr><td style=\"padding:6px 0;color:#888;\">Mật khẩu:</td><td style=\"font-weight:700;color:#1976d2;\">" + bean.getMatKhau() + "</td></tr>"
                    + "        </table>"
                    + "        <div style=\"margin-top:20px;color:#444;\">"
                    + "            Vui lòng đổi mật khẩu sau khi đăng nhập lần đầu để bảo mật tài khoản.<br>"
                    + "            <a href=\"http://localhost:3000/dang-nhap\" style=\"display:inline-block;margin-top:16px;padding:10px 32px;background:#1976d2;color:#fff;text-decoration:none;border-radius:8px;font-weight:600;box-shadow:0 2px 8px rgba(25,118,210,0.10);\">Đăng nhập ngay</a>"
                    + "        </div>"
                    + "    </div>"
                    + "</div>"
                    + "<div style=\"font-size:15px;color:#888;text-align:center;margin-top:12px;\">"
                    + "    Nếu bạn không thực hiện đăng ký này, hãy bỏ qua email này.<br>"
                    + "    <i>Đây là email tự động, vui lòng không trả lời lại.</i>"
                    + "</div>"
                    + "</div>"
                    + "</div>";
            try {
                emailConfigService.sendEmail(
                        bean.getEmail(),
                        subject,
                        body
                );
                // Ghi log khi gửi thành công
                log.info("Đã gửi email tạo tài khoản tới nhân viên: {}", bean.getEmail());
            } catch (Exception ex) {
                // THAY ĐỔI 3: Dùng logger thay vì System.err.println
                log.error("Gửi email cho nhân viên {} thất bại. Lỗi: {}", bean.getEmail(), ex.getMessage(), ex);
            }
        }

        return bean.getId();
    }

    @Transactional
    public void delete(Integer id) {
        NhanVien bean = requireOne(id);
        nhanVienRepository.delete(bean);
    }

    @Transactional
    public void update(Integer id, NhanVienUpdateVO vO, MultipartFile imageFile) {
        NhanVien bean = requireOne(id);

        BeanUtils.copyProperties(vO, bean);

        if (imageFile != null && !imageFile.isEmpty()) {
            try {
                String imageUrl = cloudinaryService.uploadImage(imageFile);
                bean.setHinhAnh(imageUrl);
            } catch (Exception e) {
                throw new RuntimeException("Không thể upload ảnh lên Cloudinary: " + e.getMessage());
            }
        }

        if (vO.getIdVaiTro() != null) {
            VaiTro vaiTro = vaiTroRepository.findById(vO.getIdVaiTro()).orElse(null);
            bean.setVaiTro(vaiTro);
        } else {
            bean.setVaiTro(null);
        }

        nhanVienRepository.save(bean);
    }

    public NhanVienDTO getById(Integer id) {
        NhanVien original = requireOne(id);
        return toDTO(original);
    }

    public Page<NhanVienDTO> query(NhanVienQueryVO vO) {
        int page = vO.getPage() != null ? vO.getPage() : 0;
        int size = vO.getSize() != null ? vO.getSize() : 5;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));


        Specification<NhanVien> spec = Specification.where(null);

        if (vO.getHoVaTen() != null && !vO.getHoVaTen().trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("hoVaTen")), "%" + vO.getHoVaTen().toLowerCase() + "%")
            );
        }
        if (vO.getGioiTinh() != null && !vO.getGioiTinh().trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("gioiTinh")), vO.getGioiTinh().toLowerCase())
            );
        }
        if (vO.getTrangThai() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("trangThai"), vO.getTrangThai())
            );
        }
        if (vO.getIdVaiTro() != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("vaiTro").get("id"), vO.getIdVaiTro())
            );
        }

        Page<NhanVien> pageResult = nhanVienRepository.findAll(spec, pageRequest);

        List<NhanVienDTO> dtos = pageResult.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageRequest, pageResult.getTotalElements());
    }

    private NhanVienDTO toDTO(NhanVien original) {
        NhanVienDTO bean = new NhanVienDTO();
        BeanUtils.copyProperties(original, bean);
        if (original.getVaiTro() != null) {
            bean.setIdVaiTro(original.getVaiTro().getId());
            bean.setTenVaiTro(original.getVaiTro().getTen());
        } else {
            bean.setIdVaiTro(null);
            bean.setTenVaiTro(null);
        }
        return bean;
    }

    private NhanVien requireOne(Integer id) {
        return nhanVienRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Resource not found: " + id));
    }
}