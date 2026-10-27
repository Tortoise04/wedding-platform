package com.wedding.service;

import com.wedding.dto.MerchantApplyRequest;
import com.wedding.dto.MerchantAuditRequest;
import com.wedding.dto.UpdateMerchantRequest;
import com.wedding.entity.Merchant;
import com.wedding.enums.AuditStatus;

import java.util.List;


// 厨师:商家入驻与审核这道菜。
public interface MerchantService {
    // 提交入驻申请(一账号一次,幂等)
    Merchant apply(Long userId, MerchantApplyRequest request);
    // 查看我的入驻进度(未提交过则报错)
    Merchant getMyApplication(Long userId);
    // 管理员审核(通过则同时改 t_merchant.audit_status 与 t_user.role)
    Merchant audit(Long merchantId, MerchantAuditRequest request);
    // 取当前用户的店铺(仅审核通过后才有)
    Merchant getMyShop(Long userId);
    // 门户:已入驻且通过审核、账号启用的商家列表
    List<Merchant> listApprovedMerchants();
    // 门户:商家详情(未过审/已停用的不对外,返回 400 不泄露)
    Merchant getMerchantById(Long id);
    // 商家端:维护本人店铺信息(简介/联系方式/经营范围/Logo)
    Merchant updateShopProfile(Long userId, UpdateMerchantRequest request);
    // 管理端:商家列表(审核状态筛选 + 关键字检索店铺名/联系电话,不传 = 全部)
    List<Merchant> listMerchantsForAdmin(AuditStatus auditStatus, String keyword);
}
