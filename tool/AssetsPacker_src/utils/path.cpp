#include "path.h"
#include "common.h"
#include "file.h"
using namespace std;

string path::name(string const &path) {
  int pos = path.length();
  while (pos > 0 && path[pos - 1] != '/' && path[pos - 1] != '\\') {
    --pos;
  }
  return path.substr(pos);
}
string path::title(string const &path) {
  size_t pos = path.length();
  size_t dot = path.length();
  while (pos && path[pos - 1] != '/' && path[pos - 1] != '\\') {
    --pos;
    if (path[pos] == '.' && dot == path.length()) {
      dot = pos;
    }
  }
  if (dot == pos) {
    return path.substr(pos);
  } else {
    return path.substr(pos, dot - pos);
  }
}
string path::path(string const &path) {
  int pos = path.length();
  while (pos > 0 && path[pos - 1] != '/' && path[pos - 1] != '\\') {
    --pos;
  }
  return path.substr(0, pos ? pos - 1 : 0);
}
string path::ext(string const &path) {
  size_t pos = path.length();
  size_t dot = path.length();
  while (pos && path[pos - 1] != '/' && path[pos - 1] != '\\') {
    --pos;
    if (path[pos] == '.' && dot == path.length()) {
      dot = pos;
    }
  }
  if (dot == pos) {
    return "";
  } else {
    return path.substr(dot);
  }
}

#ifdef _MSC_VER
#include <windows.h>
#endif
#ifndef NO_SYSTEM
#include <limits.h> // 对于 PATH_MAX
#include <string>
#include <sys/types.h>
#include <unistd.h>

using namespace std;

std::string get_parent_directory(const std::string &path) {
  size_t last_slash = path.rfind('/'); // 查找最后一个 '/'
  if (last_slash == std::string::npos) {
    // 如果没有找到 '/'，可能是一个相对路径（如 "." 或 ".."）或空字符串
    // 在这种情况下，我们可以返回 "." 作为父目录（尽管严格来说可能不准确）
    return ".";
  }
  // 如果路径以 '/' 结尾，则 last_slash 指向最后一个 '/'
  // 我们需要确保不要返回根目录的父目录（即 "/"），而是返回 "/" 本身
  if (last_slash == 0) {
    // 路径是根目录（"/"），返回 "/" 作为其“父目录”
    return "/";
  }
  // 截取最后一个 '/' 之前的所有内容
  return path.substr(0, last_slash);
}
std::string
    path::_root; // 注意：这里没有使用static关键字，因为它已经在头文件中声明过了
#include <filesystem>
void path::root(std::string const &path) {
  // 检查路径是否以斜线开头，如果不是，则视为相对路径并拼接appbase
  if (path.find('/') != 0) {
    _root = std::filesystem::absolute(appbase() / path).string();
  } else {
    _root = std::filesystem::absolute(path).string();
  }

  // 尝试切换目录（根据需要决定是否保留此逻辑）
  if (chdir(_root.c_str()) != 0) {
    perror("chdir");
  }
}
string path::root() {
  if (!_root.empty()) {
    return _root;
  }
  return path::appbase();
}
string path::appbase() {
  std::string rp;
  if (rp.empty()) {
    char buffer[PATH_MAX]; // 使用 PATH_MAX 以确保缓冲区足够大

    // 获取当前可执行文件的路径
    ssize_t bytesRead = readlink("/proc/self/exe", buffer, sizeof(buffer) - 1);
    if (bytesRead == -1) {
      perror("readlink");
      // 这里可以处理错误，比如返回一个默认路径
      rp = "~/wc3data";
    } else {
      buffer[bytesRead] = '\0'; // 添加 null 终止符
      rp = string(buffer);
      rp = get_parent_directory(rp);
      // rp = rp + "/work"; // 假设你想要的是可执行文件所在的目录下的 work
      // 子目录 std::string rpstr = "root path: " + rp; throw
      // Exception(rp.c_str()); 尝试改变当前工作目录到程序目录 if
      // (chdir(rp.c_str()) != 0) {
      //   perror("chdir");
      // }
    }

    // 注释掉或删除 Windows 特定的硬编码路径
    // rp = "C:\\Projects\\wc3data\\DataGen\\work";
    // SetCurrentDirectory(rp.c_str()); // 这个在 Linux 上不存在
  }
  return rp;
}
#endif
string operator/(string const &lhs, string const &rhs) {
  if (lhs.empty() || rhs.empty())
    return lhs + rhs;
  bool left = (lhs.back() == '\\' || lhs.back() == '/');
  bool right = (rhs.front() == '\\' || rhs.front() == '/');
  if (left && right) {
    string res = lhs;
    res.pop_back();
    return res + rhs;
  } else if (!left && !right) {
    return lhs + path::sep + rhs;
  } else {
    return lhs + rhs;
  }
}
