package org.whitesborobandboosters.rallyboro.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/** Preserve the API's arbitrary offset, rather than silently rounding to a page boundary. */
public record OffsetPage(int size, long offset, Sort sort) implements Pageable {
  public OffsetPage {
    if (size < 1 || offset < 0) throw new IllegalArgumentException("Invalid pagination");
  }

  @Override
  public int getPageNumber() {
    return (int) (offset / size);
  }

  @Override
  public int getPageSize() {
    return size;
  }

  @Override
  public long getOffset() {
    return offset;
  }

  @Override
  public Sort getSort() {
    return sort;
  }

  @Override
  public Pageable next() {
    return new OffsetPage(size, offset + size, sort);
  }

  @Override
  public Pageable previousOrFirst() {
    return new OffsetPage(size, Math.max(0, offset - size), sort);
  }

  @Override
  public Pageable first() {
    return new OffsetPage(size, 0, sort);
  }

  @Override
  public Pageable withPage(int page) {
    return new OffsetPage(size, (long) page * size, sort);
  }

  @Override
  public boolean hasPrevious() {
    return offset > 0;
  }
}
