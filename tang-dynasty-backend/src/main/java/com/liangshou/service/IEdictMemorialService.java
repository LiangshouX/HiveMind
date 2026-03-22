package com.liangshou.service;

import com.liangshou.service.dto.EdictMemorialDTO;
import com.liangshou.service.vo.EdictMemorialVO;
import com.liangshou.common.utils.PageResult;

public interface IEdictMemorialService {
    EdictMemorialVO getById(Long id);
    PageResult<EdictMemorialVO> page(int current, int size);
    boolean save(EdictMemorialDTO dto);
    boolean update(EdictMemorialDTO dto);
    boolean delete(Long id);
}
