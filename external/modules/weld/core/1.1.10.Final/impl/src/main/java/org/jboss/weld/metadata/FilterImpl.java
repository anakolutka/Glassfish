package org.jboss.weld.metadata;

import org.jboss.weld.bootstrap.spi.ClassAvailableActivation;
import org.jboss.weld.bootstrap.spi.Filter;
import org.jboss.weld.bootstrap.spi.Metadata;
import org.jboss.weld.bootstrap.spi.SystemPropertyActivation;

import java.util.Collection;


public class FilterImpl implements Filter {

    private final String pattern;
    private final String name;
    private final Collection<Metadata<SystemPropertyActivation>> systemPropertyActivation;
    private final Collection<Metadata<ClassAvailableActivation>> classAvailableActivation;

    public FilterImpl(String pattern, String name, Collection<Metadata<SystemPropertyActivation>> systemPropertyActivation, Collection<Metadata<ClassAvailableActivation>> classAvailableActivation) {
        this.pattern = pattern;
        this.name = name;
        this.systemPropertyActivation = systemPropertyActivation;
        this.classAvailableActivation = classAvailableActivation;
    }

    public String getName() {
        return name;
    }

    public String getPattern() {
        return pattern;
    }

    public Collection<Metadata<SystemPropertyActivation>> getSystemPropertyActivations() {
        return systemPropertyActivation;
    }

    public Collection<Metadata<ClassAvailableActivation>> getClassAvailableActivations() {
        return classAvailableActivation;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (this.getName() != null) {
            builder.append("name: ").append(getName());
        }
        if (this.getPattern() != null) {
            builder.append("pattern: ").append(pattern);
        }
        if (this.classAvailableActivation != null) {
            builder.append(classAvailableActivation);
        }
        if (this.systemPropertyActivation != null) {
            builder.append(systemPropertyActivation);
        }
        return builder.toString();
    }

}
