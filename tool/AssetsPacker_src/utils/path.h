#pragma once

#include <string>

namespace path {
std::string name(std::string const &path);
std::string title(std::string const &path);
std::string path(std::string const &path);
std::string ext(std::string const &path);
#ifndef NO_SYSTEM
extern std::string _root;
std::string root();
void root(std::string const &path);
std::string appbase();
#endif
#ifdef _MSC_VER
static const char sep = '\\';
#else
static const char sep = '/';

#endif
} // namespace path

std::string operator/(std::string const &lhs, std::string const &rhs);
