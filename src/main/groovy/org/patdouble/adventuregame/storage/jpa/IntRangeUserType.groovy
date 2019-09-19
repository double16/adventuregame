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
class IntRangeUserType implements UserType {
    @Override
    int[] sqlTypes() {
        [ Types.INTEGER, Types.INTEGER, Types.BOOLEAN ] as int[]
    }

    @Override
    Class returnedClass() {
        IntRange
    }

    @Override
    boolean equals(Object x, Object y) throws HibernateException {
        x == y
    }

    @Override
    int hashCode(Object x) throws HibernateException {
        x == null ? 0 : x.hashCode()
    }

    @Override
    Object nullSafeGet(ResultSet rs, String[] names, SharedSessionContractImplementor session, Object owner) throws HibernateException, SQLException {
        if (rs.wasNull()) {
            return null
        }

        int from = rs.getInt(names[0])
        int to = rs.getInt(names[1])
        Boolean inclusive = rs.getBoolean(names[2])
        if (inclusive) {
            return new IntRange(true, from, to)
        }
        return new IntRange(from, to)
    }

    @Override
    void nullSafeSet(PreparedStatement st, Object value, int index, SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.INTEGER)
            st.setNull(index+1, Types.INTEGER)
            st.setNull(index+2, Types.BOOLEAN)
        } else {
            IntRange range = value as IntRange
            st.setInt(index, range.from)
            st.setInt(index+1, range.to)
            st.setBoolean(index+2, range.inclusive)
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
