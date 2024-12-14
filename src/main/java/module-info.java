module com.brinvex.persistence {
    exports com.brinvex.persistence.api;
    requires transitive jakarta.annotation;
    requires transitive jakarta.persistence;
    requires org.hibernate.orm.core;
}