# Linux host platform module.
if(NOT CMAKE_SYSTEM_NAME STREQUAL "Linux")
  return()
endif()

message(STATUS "[VECTRAS-Linux] Applying host compile defaults")
add_compile_options(-O3 -Wall -Wextra -pedantic)
