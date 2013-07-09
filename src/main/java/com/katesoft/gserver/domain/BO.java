package com.katesoft.gserver.domain;

public interface BO {
    /**
     * @return logical primary key of the entity, not that this can't be {@link #getPrimaryKey()}.
     */
    String getPrimaryKey();
}
