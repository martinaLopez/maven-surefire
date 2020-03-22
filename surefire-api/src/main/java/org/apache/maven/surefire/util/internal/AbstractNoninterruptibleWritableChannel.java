package org.apache.maven.surefire.util.internal;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.Flushable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NonWritableChannelException;
import java.nio.channels.WritableByteChannel;

/**
 * The channel used for writes which cannot be implicitly closed after the operational Thread
 * is {@link Thread#isInterrupted() interrupted}.
 *
 * @since 3.0.0-M5
 */
abstract class AbstractNoninterruptibleWritableChannel implements WritableByteChannel, Flushable
{
    private final boolean flushable;
    private volatile boolean open = true;

    AbstractNoninterruptibleWritableChannel( boolean flushable )
    {
        this.flushable = flushable;
    }

    protected abstract void writeImpl( ByteBuffer src ) throws IOException;
    protected abstract void closeImpl() throws IOException;

    @Override
    public final synchronized int write( ByteBuffer src ) throws IOException
    {
        if ( !isOpen() )
        {
            throw new ClosedChannelException();
        }

        if ( !src.hasArray() || src.isReadOnly() )
        {
            throw new NonWritableChannelException();
        }

        if ( src.remaining() != src.capacity() )
        {
            src.flip();
        }

        int countWrittenBytes = 0;

        if ( src.hasRemaining() )
        {
            countWrittenBytes = src.remaining();
            writeImpl( src );
            src.position( src.limit() );
            if ( flushable )
            {
                flush();
            }
        }
        return countWrittenBytes;
    }

    @Override
    public final boolean isOpen()
    {
        return open;
    }

    @Override
    public final void close() throws IOException
    {
        open = false;
        closeImpl();
    }
}
