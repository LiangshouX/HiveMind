package com.liangshou.service;

import com.liangshou.service.dto.EdictMemorialDTO;
import com.liangshou.service.vo.EdictMemorialVO;
import com.liangshou.common.utils.PageResult;

public interface IEdictMemorialService {
    EdictMemorialVO getById(String userId, Long id);
    PageResult<EdictMemorialVO> page(String userId, int current, int size);
    boolean save(String userId, EdictMemorialDTO dto);
    boolean update(String userId, EdictMemorialDTO dto);
    boolean delete(String userId, Long id);
}
