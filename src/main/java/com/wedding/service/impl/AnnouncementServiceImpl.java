package com.wedding.service.impl;

import com.wedding.dto.CreateAnnouncementRequest;
import com.wedding.entity.Announcement;
import com.wedding.exception.BusinessException;
import com.wedding.repository.AnnouncementRepository;
import com.wedding.service.AnnouncementService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// 厨师:平台公告(管理端发布 / 上下线,门户只读已发布)。
//
// 要不要事务只看一条:方法里有 save 就开,纯只读的不开。
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    // 公告状态取值:0 草稿 / 1 已发布
    private static final int DRAFT = 0;
    private static final int PUBLISHED = 1;

    private final AnnouncementRepository announcementRepository;

    // 【管理端】发布公告:校验标题 → 决定发布 / 草稿 → 搬字段保存。
    //
    // publisherId 取自 token(adminId),请求体没有这个字段。
    // ★ publishTime 由服务端盖章:仅"现在就发"才写时间,存草稿留空(草稿从没对外露过脸)。
    @Transactional
    @Override
    public Announcement publishAnnouncement(Long adminId, CreateAnnouncementRequest request) {

        // ① 标题必填,校验放最前,不合法直接拒绝
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("公告标题不能为空");
        }

        // ② 决定这次是发布还是存草稿(缺省即草稿)
        Integer status = request.getStatus();                 // 可为 null
        int finalStatus = (status == null) ? DRAFT : status;  // 缺省 = 草稿

        // 前端可能传脏值,写库前打回
        if (finalStatus != DRAFT && finalStatus != PUBLISHED) {
            throw new BusinessException("公告状态只能是 1(发布)或 0(草稿)");
        }

        // ③ 创建并保存(★ 发布人来自 token,不是请求体)
        Announcement a = new Announcement();
        a.setPublisherId(adminId);
        a.setTitle(request.getTitle().trim());
        a.setContent(request.getContent());
        a.setStatus(finalStatus);
        a.setPublishTime(finalStatus == PUBLISHED ? LocalDateTime.now() : null);   // ★ 发布才盖章

        return announcementRepository.save(a);
    }

    // 【管理端】公告列表:status 传 null 即草稿 + 已发布全要。
    //
    // 按 createdAt 排(管理端关心"最近写了啥";草稿没有 publishTime,按它排会全沉到底)。
    // 门户端按 publishTime —— 排序依据不同是故意的,不是笔误。
    @Override
    public List<Announcement> listForAdmin(Integer status) {
        Specification<Announcement> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (status != null) {
                conditions.add(cb.equal(root.get("status"), status));
            }
            query.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
            return cb.and(conditions.toArray(new Predicate[0]));
        };
        return announcementRepository.findAll(spec);
    }

    // 【门户】已发布公告列表:故意不收 status 参数,值写死为 1。
    //
    // 门户只该看见已发布内容,不给调用方"顺手查草稿"的机会。
    @Override
    public List<Announcement> listPublished() {
        return announcementRepository.findByStatusOrderByPublishTimeDescIdDesc(PUBLISHED);
    }

    // 【门户】公告详情。
    //
    // ⚠️ 列表只扫标题,详情会交出整篇正文:除查出记录外必须再复核 status = 1。
    //    接口可被直接调用,不能假设调用方是从列表点进来的。
    @Override
    public Announcement getPublishedDetail(Long id) {
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("公告不存在: " + id));
        if (!Objects.equals(a.getStatus(), PUBLISHED)) {
            throw new BusinessException("该公告尚未发布");
        }
        return a;
    }

    // 【管理端】上线 / 下线:改已有数据,先 findById 取原对象再 save,故要事务。
    //
    // ★ 下线再上线不刷新 publishTime —— 仅"上线且从没发布过"(publishTime 为 null)时才补章。
    //   否则"发布时间"会退化成"最后一次点上线的时间",失去意义。
    @Transactional
    @Override
    public Announcement changeStatus(Long id, Integer status) {
        if (status == null || (status != DRAFT && status != PUBLISHED)) {
            throw new BusinessException("状态只能是 1(上线)或 0(下线)");
        }
        Announcement a = announcementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("公告不存在: " + id));

        a.setStatus(status);
        if (status == PUBLISHED && a.getPublishTime() == null) {
            a.setPublishTime(LocalDateTime.now());   // 首次发布补章
        }
        return announcementRepository.save(a);
    }
}
