/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.tomee.webapp.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.tomee.webapp.TomeeException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class CommandExecutor extends HttpServlet {

    public interface Executor {
        void call(Map<String, Object> json) throws Exception;
    }

    private List<Command> getCommands(final HttpServletRequest req) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        final String strCmds = req.getParameter("cmd");
        if (strCmds == null || "".equals(strCmds.trim())) {
            return Collections.emptyList();
        }

        final List<Command> result = new ArrayList<Command>();
        final String[] arrCmds = strCmds.split(",");
        for (String strCmd : arrCmds) {
            final Class<?> cls = Class.forName("org.apache.tomee.webapp.command.impl." + strCmd);
            final Command cmd = (Command) cls.newInstance();
            result.add(cmd);
        }

        return result;
    }

    public void execute(final HttpServletRequest req, final HttpServletResponse resp) {
        try {
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            final Gson gson;
            if (Boolean.valueOf(req.getParameter("pretty"))) {
                gson = new GsonBuilder().setPrettyPrinting().create();
            } else {
                gson = new Gson();
            }

            final List<Command> commands = getCommands(req);
            final Params params = new Params(req, resp);

            final Map<String, Object> result = new HashMap<String, Object>();
            for (Command command : commands) {
                result.put(command.getClass().getSimpleName(), command.execute(params));
            }
            resp.getWriter().write(gson.toJson(result));

        } catch (Throwable e) {
            //this will redirect the result to the ErrorServlet
            throw new TomeeException(e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execute(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execute(req, resp);
    }
}
