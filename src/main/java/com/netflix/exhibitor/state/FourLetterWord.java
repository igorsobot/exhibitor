package com.netflix.exhibitor.state;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.netflix.exhibitor.ExhibitorConfig;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class FourLetterWord
{
    private final String response;

    public enum Word
    {
        STAT,
        RUOK,
        ENVI,
        REQS,
        DUMP,
        CONF,
        CONS,
        SRVR,
        WCHS,
        WCHC,
        WCHP
    }

    public FourLetterWord(Word word, ExhibitorConfig config)
    {
        Preconditions.checkNotNull(word);

        String  localResponse = "";
        Socket  s = null;
        try
        {
            s = new Socket("localhost", config.getClientPort());
            s.setTcpNoDelay(true);
            s.setSoTimeout(config.getConnectionTimeoutMs());

            s.getOutputStream().write(word.name().toLowerCase().getBytes());
            s.getOutputStream().flush();

            localResponse = CharStreams.toString(new InputStreamReader(s.getInputStream()));
        }
        catch ( Exception e )
        {
            // ignore - treat as server not running
        }
        finally
        {
            try
            {
                if ( s != null )
                {
                    s.close();
                }
            }
            catch ( IOException e )
            {
                // ignore
            }
        }

        response = localResponse;
    }

    public List<String> getResponseLines()
    {
        ImmutableList.Builder<String> builder = ImmutableList.builder();
        BufferedReader                in = new BufferedReader(new StringReader(response));
        for(;;)
        {
            try
            {
                String      line = in.readLine();
                if ( line == null )
                {
                    break;
                }
                builder.add(line);
            }
            catch ( IOException ignore )
            {
                break;  // will never happen
            }
        }
        return builder.build();
    }

    public Map<String, String>  getResponseMap()
    {
        ImmutableMap.Builder<String, String>    builder = ImmutableMap.builder();
        for ( String line : getResponseLines() )
        {
            int     colonIndex = line.indexOf(':');
            if ( colonIndex > 0 )
            {
                String  name = line.substring(0, colonIndex);
                String  value = ((colonIndex + 1) < line.length()) ? line.substring(colonIndex + 1) : "";
                builder.put(name.toLowerCase(), value);
            }
        }

        return builder.build();
    }

    public String getResponse()
    {
        return response;
    }
}