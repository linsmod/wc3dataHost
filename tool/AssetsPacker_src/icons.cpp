#include "icons.h"
#include "hash.h"
#include "rmpq/common.h"
#include <iostream> // 包含输入输出流库
static int i = 0;
static int off = 0;
void ImageStorage::add(uint64 hash, Image image) {
  i++;
  // std::cout << fmtstring("ImageStorage::add %u. image.resize w=%u h=%u",
  // i-off,image.width(), image.height()) << std::endl;
  hashes_.push_back(hash);
  image = image.resize(width_, height_);
  if (!current_) {
    current_ = Image(width_ * cols_, height_ * rows_);
    x_ = 0;
    y_ = 0;
  }
  current_.blt(x_ * width_, y_ * height_, image);
  x_ += 1;
  if (x_ >= cols_) {
    x_ = 0;
    y_ += 1;
  }
  if (y_ >= rows_) {
    flush();
  }
}
#include "utils/logger.h"
#include "utils/path.h"
void ImageStorage::flush() {
  if (current_) {
    current_.write(path::root() / fmtstring("icons%u.png", id_++));
    current_ = Image();
    Logger::info("flush icons%u.png", id_);
    off = i;
  }
}
