/*
 * The MIT License
 *
 *   Copyright (c) 2021, Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *   FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *   AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *   LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *   OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 */
package org.jeasy.batch.extensions.hibernate;

import org.jeasy.batch.core.record.Batch;
import org.jeasy.batch.core.record.Record;
import org.jeasy.batch.core.writer.RecordWriter;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jeasy.batch.core.util.Utils.checkNotNull;

/**
 * Writes entities to a database using Hibernate.
 *
 * @author Mahmoud Ben Hassine (mahmoud.benhassine@icloud.com)
 * @param <P> type of the record's payload
 */
public class HibernateRecordWriter<P> implements RecordWriter<P> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateRecordWriter.class.getSimpleName());

    private SessionFactory sessionFactory;
    private Session session;

    /**
     * Create a new {@link HibernateRecordWriter}.
     *
     * @param sessionFactory to create sessions.
     */
    public HibernateRecordWriter(final SessionFactory sessionFactory) {
        checkNotNull(sessionFactory, "session factory");
        this.sessionFactory = sessionFactory;
    }

    @Override
    public void open() {
        LOGGER.debug("Opening a Hibernate session");
        session = sessionFactory.openSession();
    }

    @Override
    public void writeRecords(Batch<P> batch) {
        Transaction transaction = session.getTransaction();
        transaction.begin();
        try {
            for (Record<P> record : batch) {
                session.saveOrUpdate(record.getPayload());
            }
            session.flush();
            session.clear();
            transaction.commit();
            LOGGER.debug("Transaction committed");
        } catch (Exception e) {
            LOGGER.error("Unable to commit transaction", e);
            transaction.rollback();
            throw e;
        }
    }

    @Override
    public void close() {
        try {
            if (session != null) {
                LOGGER.debug("Closing Hibernate session");
                session.close();
            }
        } catch (HibernateException e) {
            LOGGER.error("Unable to close Hibernate session", e);
        }
    }
}
