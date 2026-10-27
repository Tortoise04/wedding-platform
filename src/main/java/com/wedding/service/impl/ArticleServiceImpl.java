package com.wedding.service.impl;

import com.wedding.dto.CreateArticleRequest;
import com.wedding.entity.Article;
import com.wedding.exception.BusinessException;
import com.wedding.repository.ArticleRepository;
import com.wedding.service.ArticleService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// 厨师:婚庆资讯(与公告模块同构)。
//
// 管理端发布 / 上下线,门户只读已发布;唯一特殊点是 getPublishedDetail ——
// 读详情时顺手把阅读量 +1(见该方法的注释)。
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private static final int DRAFT = 0;
    private static final int PUBLISHED = 1;

    private final ArticleRepository articleRepository;

    // 【管理端】发布资讯:校验标题 → 决定发布 / 草稿 → 搬字段保存。
    //
    // ★ viewCount 必须显式设 0 —— 数据库列 NOT NULL 且无默认值,留 null 会在 insert 时报错。
    // ★ publisherId 取自 token(adminId),请求体没有;publishTime 规则同公告:发布才盖章。
    @Transactional
    @Override
    public Article publishArticle(Long adminId, CreateArticleRequest request) {
        // ① 校验标题:空或纯空白都拒
        if (request.getTitle() == null || request.getTitle().isBlank()) {
        throw new BusinessException("资讯标题不能为空");
    }

        // ② 决定这次是发布还是存草稿(缺省即草稿)
        Integer status = request.getStatus();                 // 可为 null
        int finalStatus = (status == null) ? DRAFT : status;  // 缺省 = 草稿

        // 前端可能传脏值,写库前打回
        if (finalStatus != DRAFT && finalStatus != PUBLISHED) {
            throw new BusinessException("资讯状态不合法");
        }

        // ③ 创建并保存(★ 发布人来自 token,不是请求体)
        Article a = new Article();
        a.setCoverUrl(request.getCoverUrl());
        a.setAuthor(request.getAuthor());
        a.setViewCount(0);
        a.setPublisherId(adminId);
        a.setTitle(request.getTitle().trim());
        a.setContent(request.getContent());
        a.setStatus(finalStatus);
        a.setPublishTime(finalStatus == PUBLISHED ? LocalDateTime.now() : null);   // ★ 发布才盖章

        return articleRepository.save(a);

    }

    // 【管理端】资讯列表:status 可筛,按创建时间倒序(含草稿),与公告管理端同套路。
    @Override
    public List<Article> listForAdmin(Integer status) {
        Specification<Article> spec = (root, query, cb) -> {
            List<Predicate> conditions = new ArrayList<>();
            if (status != null) {
                conditions.add(cb.equal(root.get("status"), status));
            }
            query.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
            return cb.and(conditions.toArray(new Predicate[0]));
        };
        return articleRepository.findAll(spec);
    }

    // 【门户】已发布资讯:status 写死 1,不给调用方"顺手看草稿"的机会。
    @Override
    public List<Article> listPublished() {
        return articleRepository.findByStatusOrderByPublishTimeDescIdDesc(PUBLISHED);
    }

    // 【门户】热门资讯:按阅读量倒序 —— 资讯独有,公告没有阅读量自然也没有"热门"。
    @Override
    public List<Article> listHot() {
        return articleRepository.findByStatusOrderByViewCountDescIdDesc(PUBLISHED);
    }

    // 【门户】资讯详情 + 阅读量 +1 —— 本项目第一个"来读一下,却顺手改了库"的接口。
    //
    // 禁止读-改-写("findById → setViewCount(+1) → save"),并发下会丢失更新;
    // 阅读量自增必须交给数据库:increaseViewCount 是 @Modifying(clearAutomatically = true) 的原子 update。
    // ⚠️ @Transactional 必须在:@Modifying 语句无事务会直接抛异常,自增与重查也要同一事务。
    @Transactional
    @Override
    public Article getPublishedDetail(Long id) {
        // 该 update 自带 where id = ? and status = 1,兼验"存在 + 已发布";影响行数 0 即拒绝
        int rows = articleRepository.increaseViewCount(id);
        if (rows == 0) {
            throw new BusinessException("资讯不存在或尚未发布: " + id);
        }
        // 再查一次拿"加过之后"的值(clearAutomatically 会清一级缓存,故这里真回库取新值)
        return articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("资讯不存在: " + id));
    }

    // 【管理端】上线 / 下线:与公告同规则,下线再上线不刷新 publishTime。
    @Transactional
    @Override
    public Article changeStatus(Long id, Integer status) {
        if (status == null || (status != DRAFT && status != PUBLISHED)) {
            throw new BusinessException("状态只能是 1(上线)或 0(下线)");
        }
        Article a = articleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("资讯不存在: " + id));

        a.setStatus(status);
        if (status == PUBLISHED && a.getPublishTime() == null) {
            a.setPublishTime(LocalDateTime.now());
        }
        return articleRepository.save(a);
    }
}
