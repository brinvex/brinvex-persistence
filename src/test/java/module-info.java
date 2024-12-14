module test.brinvex.persistence {
    requires com.brinvex.persistence;
    requires org.hibernate.orm.core;
    requires org.junit.jupiter.api;
    requires org.junit.jupiter.engine;
    requires org.slf4j;
    opens test.com.brinvex.persistence.infra to org.junit.platform.commons;
    opens test.com.brinvex.persistence to org.junit.platform.commons;
    opens test.com.brinvex.persistence.dm to org.hibernate.orm.core;
    opens test.com.brinvex.persistence.dao to org.junit.platform.commons;

}