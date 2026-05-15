cmake_minimum_required(VERSION 3.22.1)
project("lipton_native")

add_library(lipton_native SHARED src/main/cpp/native_spawn.c)

find_library(log-lib log)
target_link_libraries(lipton_native ${log-lib})
