package com.liangshou.infrastructure.datasource.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户表
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user")
public class SysUserPO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**

     * 主键

     */

    @TableId(type = IdType.AUTO)
    private Long id;

    /**

     * 用户名

     */

    @TableField("user_name")

    private String userName;

    /**

     * 用户电话号码，包含国际区号

     */

    @TableField("phone_number")

    private String phoneNumber;

    /**

     * 密码哈希

     */

    @TableField("password")

    private String password;

    /**

     * 昵称

     */

    @TableField("nickname")

    private String nickname;

    /**

     * 角色

     */

    @TableField("role")

    private String role;

    /**

     * 微信应用唯一标识

     */

    @TableField("wechat_openid")

    private String wechatOpenid;

    /**

     * 微信开放平台统一标识

     */

    @TableField("wechat_unionid")

    private String wechatUnionid;

    /**

     * 微信昵称

     */

    @TableField("wechat_nickname")

    private String wechatNickname;

    /**

     * 微信头像URL

     */

    @TableField("wechat_avatar")

    private String wechatAvatar;

    /**

     * 性别: 0-未知 1-男 2-女

     */

    @TableField("gender")

    private Integer gender;

    /**

     * 逻辑删除

     */

    @TableLogic

    @TableField("deleted")

    private Integer deleted;

    /**

     * 乐观锁版本

     */

    @Version

    @TableField("version")

    private Integer version;

    /**

     * 创建时间

     */

    @TableField(fill = FieldFill.INSERT, value = "create_time")

    private LocalDateTime createTime;

    /**

     * 更新时间

     */

    @TableField(fill = FieldFill.INSERT_UPDATE, value = "update_time")

    private LocalDateTime updateTime;

}
