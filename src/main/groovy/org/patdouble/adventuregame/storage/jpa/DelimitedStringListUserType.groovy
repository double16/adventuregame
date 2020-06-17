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
 * Hibernate user type for storing a list of strings in a single delimited column. The user of the type is responsible
 * for ensuring the deliminator isn't contained in the values.
 */
@CompileStatic
@SuppressWarnings(['Unused', 'JdbcResultSetReference', 'JdbcStatementReference'])
class DelimitedStringListUserType implements UserType {
    private static final String DELIMINATOR = ';'

    @Override
    int[] sqlTypes() {
        [ Types.VARCHAR ] as int[]
    }

    @Override
    Class returnedClass() {
        List
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

        String delimited = rs.getString(names[0])
        if (rs.wasNull()) {
            return []
        }
        delimited.split(DELIMINATOR) as List
    }

    @Override
    void nullSafeSet(
            PreparedStatement st,
            Object value,
            int index,
            SharedSessionContractImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            st.setNull(index, Types.LONGVARCHAR)
        } else {
            st.setString(index, (value as Collection).join(DELIMINATOR))
        }
    }

    @Override
    Object deepCopy(Object value) throws HibernateException {
        new ArrayList(value as Collection)
    }

    @Override
    boolean isMutable() {
        true
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
