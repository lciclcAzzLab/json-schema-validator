/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of both licenses is available under the src/resources/ directory of
 * this project (under the names LGPL-3.0.txt and ASL-2.0.txt respectively).
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.jsonschema.main;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.processors.syntax.SyntaxValidator;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import joptsimple.HelpFormatter;
import joptsimple.OptionDescriptor;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Main
{
    private static final String LINE_SEPARATOR
        = System.getProperty("line.separator", "\n");
    private static final Joiner JOINER = Joiner.on(LINE_SEPARATOR);
    private static final Function<String, String> ADD_NEWLINE
        = new Function<String, String>()
    {
        @Nullable
        @Override
        public String apply(@Nullable final String input)
        {
            return Optional.fromNullable(input).or("") + '\n';
        }
    };
    private static final Joiner OPTIONS_JOINER = Joiner.on(", ");
    private static final String HELP_PREAMBLE
        = "Syntax: java -jar jsonschema.jar [options] file [file...]";

    private static final HelpFormatter HELP = new CustomHelpFormatter();

    private final HelpFormatter helpFormatter = new CustomHelpFormatter();
    private final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();


    public static void main(final String... args)
        throws IOException
    {
        final OptionParser parser = new OptionParser();
        parser.accepts("syntax",
            "check the syntax of schema(s) given as argument(s)");
        parser.accepts("help", "show this help").forHelp();
        parser.formatHelpWith(HELP);

        final OptionSet optionSet;
        final boolean isSyntax;
        final int requiredArgs;

        try {
            optionSet = parser.parse(args);
            if (optionSet.has("help")) {
                parser.printHelpOn(System.out);
                System.exit(0);
            }
        } catch (OptionException e) {
            System.err.println("unrecognized option(s): "
                + OPTIONS_JOINER.join(e.options()));
            parser.printHelpOn(System.err);
            System.exit(2);
            throw new IllegalStateException("WTF??");
        }

        isSyntax = optionSet.has("syntax");
        requiredArgs = isSyntax ? 1 : 2;

        @SuppressWarnings("unchecked")
        final List<String> arguments
            = (List<String>) optionSet.nonOptionArguments();

        if (arguments.size() < requiredArgs) {
            System.err.println("missing arguments");
            parser.printHelpOn(System.err);
            System.exit(2);
        }

        final String input = "{" +
            "\"$schema\": \"http://json-schema.org/draft-04/hyper-schema#\"," +
            "\"links\":null" +
            "}";
        final JsonNode node = JsonLoader.fromString(input);
        final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
        final SyntaxValidator validator = factory.getSyntaxValidator();
        final ProcessingReport report = validator.validateSchema(node);
        System.out.println(report);
    }

    private static final class CustomHelpFormatter
        implements HelpFormatter
    {
        private final List<String> lines = Lists.newArrayList();

        @Override
        public String format(
            final Map<String, ? extends OptionDescriptor> options)
        {
            final Set<OptionDescriptor> opts
                = new LinkedHashSet<OptionDescriptor>(options.values());

            lines.add(HELP_PREAMBLE);
            lines.add("");
            lines.add("Options: ");

            StringBuilder sb;

            for (final OptionDescriptor descriptor: opts) {
                if (descriptor.representsNonOptions())
                    continue;
                sb = new StringBuilder().append('\t')
                    .append(optionsToString(descriptor.options()))
                    .append(": ").append(descriptor.description());
                lines.add(sb.toString());
            }

            lines.add("");
            lines.add("Exit codes:");
            lines.add("\t0: validation successful;");
            lines.add("\t1: exception occurred (appears on stderr)");
            lines.add("\t2: command line syntax error (missing argument, etc)");
            lines.add("\t100: one or more file(s) failed validation");

            return JOINER.join(lines) + LINE_SEPARATOR;
        }

        private String optionsToString(final Collection<String> names)
        {
            final List<String> list = Lists.newArrayList();
            for (final String name: names)
                list.add("--" + name);
            return OPTIONS_JOINER.join(list);
        }
    }
}
