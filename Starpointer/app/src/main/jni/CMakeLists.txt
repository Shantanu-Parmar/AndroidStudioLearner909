cmake
cmake_minimum_required(VERSION 3.18.1)
project(Starpointer)
set(NCNN_ROOT_DIR ${CMAKE_SOURCE_DIR}/ncnn-20250503-android-shared)
set(ncnn_DIR ${NCNN_ROOT_DIR}/${ANDROID_ABI} CACHE PATH "Path to NCNN specific ABI directory")
if(NOT EXISTS ${ncnn_DIR})
    message(FATAL_ERROR "NCNN directory for ABI ${ANDROID_ABI} not found at ${ncnn_DIR}")
endif()

add_library(ncnn STATIC IMPORTED)
set_target_properties(ncnn PROPERTIES IMPORTED_LOCATION ${ncnn_DIR}/lib/libncnn.a)
set(OpenCV_DIR "C:/Users/HP/opencv-4.12.0-android-sdk/OpenCV-android-sdk/sdk/native/jni") # <--- MODIFIED PATH

# Check if the OpenCV directory exists
if(NOT EXISTS ${OpenCV_DIR})
    message(FATAL_ERROR "OpenCV_DIR not found at ${OpenCV_DIR}. Please check the path.")
endif()

# Find OpenCV package
find_package(OpenCV REQUIRED
    # You might need to specify components if you only need a subset of OpenCV
    # For example: core imgproc
    # COMPONENTS core imgproc
)

if(NOT OpenCV_FOUND)
    message(FATAL_ERROR "OpenCV not found. Check OpenCV_DIR and your OpenCV installation.")
endif()

# --- Main Native Library ---
add_library(nanodet SHARED
            nanodet.cpp
            # Add other .cpp files for your nanodet library here
            )
target_include_directories(nanodet PRIVATE
    ${ncnn_DIR}/include # NCNN headers
    ${OpenCV_INCLUDE_DIRS} # OpenCV headers
    # Add any other include directories your nanodet.cpp needs
)


target_link_libraries(nanodet PRIVATE
    ncnn                # Your imported NCNN static library
    ${OpenCV_LIBS}      # OpenCV libraries found by find_package
    vulkan              # Vulkan library (make sure NDK supports this for your target)
    android             # For Android-specific APIs like logging
    log                 # For __android_log_print
    # Add other libraries if needed, e.g., EGL, GLESv2, GLESv3
)
