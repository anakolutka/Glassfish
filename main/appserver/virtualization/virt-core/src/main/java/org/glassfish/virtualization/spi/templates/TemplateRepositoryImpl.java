/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.virtualization.spi.templates;

import com.sun.enterprise.util.io.FileUtils;
import com.sun.logging.LogDomains;
import org.glassfish.virtualization.config.Template;
import org.glassfish.virtualization.config.Virtualizations;
import org.glassfish.virtualization.spi.SearchCriteria;
import org.glassfish.virtualization.spi.TemplateCondition;
import org.glassfish.virtualization.spi.TemplateInstance;
import org.glassfish.virtualization.spi.TemplateRepository;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the {@link TemplateRepository} interface
 * @author Jerome Dochez
 */
@Service
public class TemplateRepositoryImpl implements TemplateRepository {

    final File location;
    final Logger logger = LogDomains.getLogger(TemplateRepositoryImpl.class, LogDomains.VIRTUALIZATION_LOGGER);
    final List<TemplateInstance> templates = new ArrayList<TemplateInstance>();

    public TemplateRepositoryImpl(@Inject Virtualizations virts) {
        location = new File(virts.getTemplatesLocation());
        for (Template template : virts.getTemplates()) {
            templates.add(new TemplateInstanceImpl(template));
        }
    }

    @Override
    public boolean installs(Template config, Collection<File> files) {
        String templateName = config.getName();
        File templateLocation = new File(location, templateName);
        if (templateLocation.exists()) {
            if (!FileUtils.whack(templateLocation)) {
                logger.severe("Template not installed, cannot delete existing template directory "
                        + templateLocation.getAbsolutePath());
                return false;
            }
        }
        if (!templateLocation.mkdirs()) {
            logger.severe("Cannot create template location directory at " + templateLocation.getAbsolutePath());
            return false;
        }
        for (File f : files) {
            try {
                FileUtils.copy(f, new File(templateLocation, f.getName()));
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Cannot copy file " + f.getAbsolutePath(), e);
                FileUtils.whack(templateLocation);
                return false;
            }
        }
        TemplateInstance templateInstance = new TemplateInstanceImpl(config);
        templates.add(templateInstance);
        return true;
    }

    @Override
    public Collection<TemplateInstance> get(SearchCriteria criteria) {
        // first all the mandatory
        List<TemplateInstance> candidates = new ArrayList<TemplateInstance>(templates);
        for (TemplateCondition templateIndex : criteria.and()) {
            for (TemplateInstance templateInstance : new ArrayList<TemplateInstance>(candidates)) {
                if (!templateInstance.satisfies(templateIndex)) {
                    candidates.remove(templateInstance);
                }
            }
        }
        // now the OR.
        for (TemplateInstance templateInstance : new ArrayList<TemplateInstance>(candidates)) {
            boolean foundOne=false;
            for (TemplateCondition templateIndex : criteria.or()) {
                if (templateInstance.satisfies(templateIndex)) {
                    foundOne=true;
                    break;
                }
            }
            if (!foundOne) candidates.remove(templateInstance);
        }
        // todo optionals
        return candidates;
    }

    @Override
    public Collection<TemplateInstance> all() {
        return Collections.unmodifiableCollection(templates);
    }
}
