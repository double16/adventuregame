package org.patdouble.adventuregame.storage.jpa

import groovy.transform.CompileStatic
import org.hibernate.HibernateException
import org.hibernate.engine.spi.SharedSessionContractImplementor
import org.hibernate.usertype.UserType

import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Types

/**
 * Hibernate user type for storing a range of integers.
 */
@CompileStatic
@SuppressWarnings(['JdbcResultSetReference', 'JdbcStatementReference'])
class IntRangeUserType implements UserType {

    private static final int OFFSET_FROM = 0
    private static final int OFFSET_TO = 1
    private static final int OFFSET_INCLUSIVE = 2

    @Override
    int[] sqlTypes() {
        [ Types.INTEGER, Types.INTEGER, Types.BOOLEAN ] as int[]
    }

    @Override
    Class returnedClass() {
        IntRange
    }

    @Override
    @SuppressWarnings('EqualsOverloaded')
    boolean equals(Object x, Object y) throws HibernateException {
        x == y
    }

    @Override
    int hashCode(Object x) throws HibernateException {
        x == null ? 0 : x.hashCode()
    }

    @Override
    Object nullSafeGet(
            ResultSet rs,
            String[] names,
            SharedSessionContractImplementor session,
            Object owner) throws HibernateException, SQLException {
        if (rs.wasNull()) {
            return null
        }

        int from = rs.getInt(names[OFFSET_FROM])
        int to = rs.getInt(names[OFFSET_TO])
        Boolean inclusive = rs.getBoolean(names[OFFSET_INCLUSIVE])
        if (inclusive) {
            return new IntRange(true, from, to)
        }
        return new IntRange(from, to)
    }

    @Override
    void nullSafeSet(
            PreparedStatement st,
            Object value,
            int index,
            SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index + OFFSET_FROM, Types.INTEGER)
            st.setNull(index + OFFSET_TO, Types.INTEGER)
            st.setNull(index + OFFSET_INCLUSIVE, Types.BOOLEAN)
        } else {
            IntRange range = value as IntRange
            st.setInt(index + OFFSET_FROM, range.from)
            st.setInt(index + OFFSET_TO, range.to)
            st.setBoolean(index + OFFSET_INCLUSIVE, range.inclusive)
        }
    }

    @Override
    Object deepCopy(Object value) throws HibernateException {
        value
    }

    @Override
    boolean isMutable() {
        false
    }

    @Override
    @SuppressWarnings('Instanceof')
    Serializable disassemble(Object value) throws HibernateException {
        Object deepCopy = deepCopy(value)
        if (!(deepCopy instanceof Serializable)) {
            return (Serializable) deepCopy
        }
        return null
    }

    @Override
    Object assemble(Serializable cached, Object owner) throws HibernateException {
        deepCopy(cached)
    }

    @Override
    Object replace(Object original, Object target, Object owner) throws HibernateException {
        deepCopy(original)
    }
}
