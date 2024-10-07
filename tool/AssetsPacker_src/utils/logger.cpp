#include "logger.h"
#include "common.h"
#include <algorithm>
#include <list>
#include <ncurses.h>
#include <ostream>
#include <termios.h>

// 移除Windows相关的预处理器指令和包含
// #ifdef _MSC_VER
// #define NOMINMAX
// #include <conio.h>
// #include <windows.h>
// ...
// #endif

// 由于NOLOGGER在这里未被使用，可以考虑移除或根据实际用途保留
// #ifdef _MSC_VER
// #else
// #define NOLOGGER
// ...
// #endif

// 使用ncurses的窗口句柄类型，这部分代码可以保留，因为它已经是POSIX兼容的
typedef WINDOW *HANDLE;
typedef void VOID;
typedef unsigned long DWORD;

// COORD 结构体在ncurses中已有对应定义，无需重复定义
// struct COORD { SHORT X, Y; };

// 直接使用ncurses的函数，因此GetStdHandle等可以省略或根据需要调整
// static const int STD_OUTPUT_HANDLE = 0;
// HANDLE GetStdHandle(int) {
//    return _consoleHandle;
// }

// 初始化和结束ncurses环境
void ConsoleStart() {
  initscr();
  start_color();
  for (int i = 1; i <= 7; ++i) {
    init_pair(i, i, COLOR_BLACK);
  }
  attron(COLOR_PAIR(7));
}

void ConsoleEnd() { endwin(); }

// 设置文本属性
void SetConsoleTextAttribute(HANDLE console, int color) {
  // if (color != _consoleColor) {
  //     attroff(COLOR_PAIR(_consoleColor));
  //     attron(COLOR_PAIR(_consoleColor = color));
  // }
}

// 获取光标位置的模拟在ncurses中通过getcury和getcurx实现，这部分代码可以省略或内联到需要使用的地方
// struct CONSOLE_SCREEN_BUFFER_INFO {
//     COORD dwCursorPosition;
// };
// void GetConsoleScreenBufferInfo(HANDLE console, CONSOLE_SCREEN_BUFFER_INFO*
// info) {
//     ...
// }

// 对于_getch函数，使用termios进行非缓冲输入的处理已经正确，保持不变
int _getch() {
  struct termios t1, t2;
  tcgetattr(0, &t1);
  t2 = t1;
  t2.c_lflag &= ~ICANON;
  t2.c_lflag &= ~ECHO;
  tcsetattr(0, TCSANOW, &t2);
  int ch = getchar();
  tcsetattr(0, TCSANOW, &t1);
  return ch;
}

// 注意：原始代码中的WriteConsole和SetConsoleCursorPosition函数已经在ncurses环境中通过wprintw,
// wmove等函数间接实现，
// 因此，如果这些函数在其他地方被调用，应该直接使用ncurses提供的API。

Logger::Logger() : logfile(nullptr) {
  this->currentLogLevel = LogLevel::ALL;
#ifndef NOLOGGER
  ConsoleStart();
#endif
}
Logger::~Logger() {
#ifndef NOLOGGER
  ConsoleEnd();
#endif
  delete logfile;
}

Logger Logger::instance;

struct Logger::Task {
  enum {
    cLog = -1,
    cMenu = -2,
    cMenuOption = -3,
  };

  HANDLE handle;
  Task *parent;
  std::string name;
  std::string line;
  int count;
  int index;
  int height;
  std::string menu;
  int msize;
  uint32 time;
  std::list<Task> sub;
  typedef std::list<Task>::iterator Iter;

  Task();
  Task(Task *parent, int count, std::string const &name);
  ~Task();

  void erase();
  void draw();
  void write(std::string const &text);
  void move(int y);
  void item(char const *text);
  void progress(int count, bool add = true);
  void close();
  Iter find(Task *task);
  void update(Iter from);
  void shift(Iter from);
  void rupdate(Iter from);
  void rshift(Iter from);
  void remove(Task *task);
  Task *insert(int count, std::string const &name);
};

static Logger::Task _root;
Logger::Task *Logger::root = &_root;
Logger::Task *Logger::top = nullptr;

void Logger::Task::move(int y) {
  erase();
  // pos.Y = y;
  draw();
}
void Logger::Task::item(char const *text) {
  ++index;
  if (GetTickCount() > time + 200) {
    write(text ? (name.empty() ? text : fmtstring("%s: %s", name.c_str(), text))
               : name);
  }
}
void Logger::Task::progress(int count, bool add) {
  if (add)
    index += count;
  else
    index = count;
  if (GetTickCount() > time + 200) {
    write(name);
  }
}
void Logger::Task::erase() {
  std::string buf(8 + line.length(), ' ');
  // SetConsoleCursorPosition(handle, pos);
  // WriteConsole(handle, buf.c_str(), buf.length(), nullptr, nullptr);
  // SetConsoleCursorPosition(handle, { 0, pos.Y });
}
void Logger::Task::draw() {
  std::string prev = line;
  line.clear();
  write(prev);
}
#include <iostream> // 包含输入输出流库
void Logger::Task::write(std::string const &text) {
  std::string buf;
  if (count < 0) {
    if (count == cLog) {
      buf = "[ Log ] ";
    } else if (count == cMenu) {
      if (menu.size() == 1) {
        buf = "[  ?  ] ";
      } else if (menu.size() == 2) {
        buf = fmtstring("[  %s ] ", menu.c_str());
      } else {
        buf = fmtstring("[ %s ] ", menu.c_str());
      }
    } else if (parent->msize < 0) {
      buf = fmtstring("[ (%c) ] ", char(cMenuOption - count));
    } else if (parent->msize < 2) {
      buf = fmtstring("[ (%d) ] ", (cMenuOption - count));
    } else if (parent->msize < 3) {
      buf = fmtstring("[ (%02d)] ", (cMenuOption - count));
    } else {
      buf = fmtstring("[(%03d)] ", (cMenuOption - count));
    }
  } else if (!count || index >= count) {
    buf = "[ Done] ";
  } else {
    buf = fmtstring("[%4.1lf%%] ", 100.0 * std::max(index, 0) / count);
    if (buf.substr(0, 4) == "[100") {
      buf = "[99.9%] ";
    }
  }

  std::cout << buf << std::endl;
  line = text;
  time = GetTickCount();
}
void Logger::Task::close() {
  index = count;
  write(name);
  if (sub.size()) {
    for (auto &x : sub) {
      x.erase();
    }
    sub.clear();
    int delta = height - 1;
    for (Task *cur = this; cur; cur = cur->parent) {
      cur->height -= delta;
    }
    shift(sub.end());
  }
}
Logger::Task::Iter Logger::Task::find(Task *task) {
  for (Iter it = sub.begin(); it != sub.end(); ++it) {
    if (&*it == task)
      return it;
  }
  return sub.end();
}
void Logger::Task::update(Iter from) {
  Task *prev = nullptr;
  if (from != sub.begin()) {
    prev = &*std::prev(from);
  }
  for (; from != sub.end(); ++from) {
    // from->move(prev ? prev->pos.Y + prev->height : pos.Y + 1);
    // from->update(from->sub.begin());
    prev = &*from;
  }
}
void Logger::Task::shift(Iter from) {
  update(from);
  if (parent) {
    Iter self = parent->find(this);
    if (self != parent->sub.end()) {
      parent->shift(std::next(self));
    }
  }
}
void Logger::Task::rupdate(Iter from) {
  if (from == sub.end())
    return;
  Task *to = &*from;
  // int prev = pos.Y + height;
  // for (auto it = sub.rbegin(); it != sub.rend(); ++it) {
  //   it->rupdate(it->sub.begin());
  //   it->move(prev - it->height);
  //   // prev = it->pos.Y;
  //   if (&*it == to) break;
  // }
}
void Logger::Task::rshift(Iter from) {
  if (parent) {
    Iter self = parent->find(this);
    if (self != parent->sub.end()) {
      parent->rshift(std::next(self));
    }
  }
  rupdate(from);
}
void Logger::Task::remove(Task *task) {
  Iter pos = find(task);
  if (pos == sub.end())
    return;
  for (Task *cur = this; cur; cur = cur->parent) {
    cur->height -= task->height;
  }
  task->close();
  task->erase();
  Iter next = std::next(pos);
  sub.erase(pos);
  shift(next);
}
Logger::Task *Logger::Task::insert(int count, std::string const &name) {
  for (Task *cur = this; cur; cur = cur->parent) {
    ++cur->height;
  }
  rshift(sub.end());
  sub.emplace_back(this, count, name);
  return &sub.back();
}

Logger::Task::Task()
    : handle(0), parent(nullptr), count(0), index(0), height(1), msize(0),
      time(0) {
  // CONSOLE_SCREEN_BUFFER_INFO info;
  // GetConsoleScreenBufferInfo(handle, &info);
  // pos.X = -2;
  // pos.Y = info.dwCursorPosition.Y - 1;
}
Logger::Task::Task(Task *parent, int count, std::string const &name)
    : handle(0), parent(parent), name(name), count(count), index(-1), height(1),
      msize(0), time(0) {
  // pos.X = parent->pos.X + 2;
  if (parent->sub.empty()) {
    // pos.Y = parent->pos.Y + 1;
  } else {
    // Task* last = &parent->sub.back();
    // pos.Y = last->pos.Y + last->height;
  }
  write(name);
}
Logger::Task::~Task() {
  // if (parent) {
  //  erase();
  //}
}

#ifdef NOLOGGER
void *Logger::begin(size_t count, char const *name, void *task_) {
  for (int i = 0; i < instance.pad_left; ++i) {
    Logger::puts("  ");
  }
  Logger::puts("  ");
  Logger::puts(name);
  Logger::puts("\n");
  ++instance.pad_left;
  return nullptr;
}
void Logger::item(char const *name, void *task_) {
  for (int i = 0; i < instance.pad_left; ++i) {
    Logger::puts("  ");
  }
  Logger::puts(name);
  Logger::puts("\n");
}
void Logger::progress(size_t count, bool add, void *task_) {}
void Logger::end(bool pop, void *task_) { --instance.pad_left; }
#include "utils/path.h"
std::string getLogLevelString(LogLevel level) {
  switch (level) {
  case LogLevel::DEBUG:
    return "DEBUG";
  case LogLevel::INFO:
    return "INFO";
  case LogLevel::ERROR:
    return "ERROR";
  case LogLevel::WARN:
    return "WARN";
  default:
    return "DEBUG";
  }
}
void Logger::log(LogLevel level, char const *fmt, ...) {
  if (level <= instance.currentLogLevel) {
    va_list ap;
    va_start(ap, fmt);
    std::string text = varfmtstring(fmt, ap); // 确保这个函数可以正确处理va_list
    va_end(ap);

    varfmtstring(fmt, ap);
    Logger::item(text.c_str(), nullptr);

    
    // fprintf(stderr, "[%s] %s\n", getLogLevelString(level).c_str(),
    //         text.c_str());

    // if (!instance.logfile) {
    //   instance.logfile = new File(path::root() / "log.txt", "at");
    //   instance.logfile->printf("============\n");
    // }
    // instance.logfile->printf("[%s] %s\n", getLogLevelString(level).c_str(),
    //                          text.c_str());
  }
}
void Logger::debug(char const *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  log(LogLevel::DEBUG, varfmtstring(fmt, ap).c_str());
  va_end(ap);
}
void Logger::info(char const *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  log(LogLevel::INFO, varfmtstring(fmt, ap).c_str());
  va_end(ap);
}
void Logger::warn(char const *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  log(LogLevel::WARN, varfmtstring(fmt, ap).c_str());
  va_end(ap);
}
void Logger::error(char const *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  log(LogLevel::ERROR, varfmtstring(fmt, ap).c_str());
  va_end(ap);
}
void Logger::puts(char const *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  std::string text = varfmtstring(fmt, ap);
  va_end(ap);
  fprintf(stderr, "%s", text.c_str());

  if (!instance.logfile) {
    instance.logfile = new File(path::root() / "log.txt", "at");
    instance.logfile->printf("============\n");
  }
  instance.logfile->printf("%s", text.c_str());
}
extern int remove(const char *__filename);
void Logger::remove() {
  if (!instance.logfile) {
    ::remove((path::root() / "log.txt").c_str());
  }
}
#else
void *Logger::begin(size_t count, char const *name, void *task_) {
  Task *task = (task_ ? (Task *)task_ : top);
  if (!task)
    task = root;
  return top = task->insert(count, std::string(name ? name : ""));
}
void Logger::item(char const *name, void *task_) {
  Task *task = (task_ ? (Task *)task_ : top);
  task->item(name);
}
void Logger::progress(size_t count, bool add, void *task_) {
  Task *task = (task_ ? (Task *)task_ : top);
  task->progress(count, add);
}
void Logger::end(bool pop, void *task_) {
  Task *task = (task_ ? (Task *)task_ : top);
  if (top == task)
    top = task->parent;
  if (pop) {
    task->parent->remove(task);
  } else {
    task->close();
  }
}

void Logger::log(char const *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  std::string text = varfmtstring(fmt, ap);
  root->insert(Task::cLog, text);
  va_end(ap);
  if (!instance.logfile) {
    instance.logfile = new File("log.txt", "at");
    instance.logfile->printf("============\n");
  }
  instance.logfile->printf("%s\n", text.c_str());
}
#endif

#include <iostream>

int Logger::menu(char const *title, std::vector<std::string> const &options) {
  Task *task = root->insert(Task::cMenu, title);
  int digits = 1, mul = 1;
  if (options.size() > 99) {
    digits = 3, mul = 100;
  } else if (options.size() > 9) {
    digits = 2, mul = 10;
  }
  task->menu.resize(digits, '?');
  task->msize = digits;
  task->draw();
  for (size_t i = 0; i < options.size(); ++i) {
    task->insert(Task::cMenuOption - i - 1, options[i]);
  }
  int input = 0, pos = 0;
  do {
    int chr = _getch();
    if ((chr < '0' || chr > '9') && chr != 27 && chr != 8) {
      continue;
    }
    if (chr == 8) {
      if (!pos)
        continue;
      --pos;
      mul *= 10;
      input /= 10;
    } else if (chr == 27) {
      if (!pos)
        continue;
      while (pos) {
        --pos;
        mul *= 10;
        input /= 10;
      }
    } else {
      int next = input * 10 + (chr - '0');
      if (static_cast<size_t>(next * mul) > options.size())
        continue;
      input = next;
      mul /= 10;
      ++pos;
    }
    task->close();
    task->msize = digits - pos;
    if (pos) {
      task->menu = fmtstring("%0*d", pos, input);
    } else {
      task->menu.clear();
    }
    task->menu.resize(digits, '?');
    task->draw();
    size_t first = input * 10 * mul;
    if (!first)
      first = 1;
    size_t last = (input * 10 + 9) * mul;
    if (last > options.size())
      last = options.size();
    for (size_t i = first; i <= last; ++i) {
      task->insert(Task::cMenuOption - (i - input * 10 * mul), options[i - 1]);
    }
  } while (pos < digits);
  task->parent->remove(task);
  return input - 1;
}

int Logger::menu(char const *title,
                 std::map<char, std::string> const &options) {
  Task *task = root->insert(Task::cMenu, title);
  task->msize = -1;
  task->menu = "?";
  task->draw();
  for (auto const &kv : options) {
    task->insert(Task::cMenuOption - kv.first, kv.second);
  }

  int chr;
  do {
    chr = ::toupper(_getch());
  } while (options.find(chr) == options.end());
  task->parent->remove(task);
  return chr;
}
