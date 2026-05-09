package com.testmgmt.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates human-readable codes like TC-205, TR-88, DEF-1042
 * using PostgreSQL sequences so they are safe under concurrent load.
 */
@Service
@RequiredArgsConstructor
public class CodeSequenceService {

    private final JdbcTemplate jdbc;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String next(String prefix) {
        String seqName = "seq_" + prefix.toLowerCase().replace("-", "_");
        Long val = jdbc.queryForObject(
            "SELECT nextval('" + seqName + "')", Long.class);
        return prefix + "-" + val;
    }
}
