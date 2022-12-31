package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  private Boolean profiledClass(Class<?> klass) {
    // Get the declared methods of the class and its superclasses
    List<Method> methods = new ArrayList<>(Arrays.asList(klass.getDeclaredMethods()));
    Class<?> superclass = klass.getSuperclass();
    while (superclass != null) {
      methods.addAll(Arrays.asList(superclass.getDeclaredMethods()));
      superclass = superclass.getSuperclass();
    }

    // Return true if any of the methods are annotated with @Profiled
    return methods.stream().anyMatch(x -> x.getAnnotation(Profiled.class) != null);
  }


  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);
    if (!profiledClass(klass)) {
      throw new IllegalArgumentException(klass.getName() + "doesn't have profiled methods.");
    }

    // Create a new ProfilingMethodInterceptor instance
    ProfilingMethodInterceptor interceptor = new ProfilingMethodInterceptor(clock, state, delegate, startTime);

    // Create a dynamic proxy using the class of the delegate and the interceptor

    // Return the dynamic proxy
    return (T) Proxy.newProxyInstance(
            ProfilerImpl.class.getClassLoader(),
            new Class[]{klass},
            interceptor
    );
  }




  @Override
  public void writeData(Path path) {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.

    try (Writer writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      writeData(writer);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
