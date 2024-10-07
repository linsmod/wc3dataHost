#!/bin/bash

export out=$(pwd)/out
export obj=""
clang=gcc
clang_cpp=g++
mkdir -p $out
call() {
    local ts="$4.timestamp"
    touch $ts
    # cpp timestamp
    local ifts=$(stat -c %Y "$2")
    # with build command
    local info="$@ $ifts"
    local old_info=$(<"$ts")
    if [ "$info" == "$old_info" ] && [ -f "$4" ]; then
        echo "[skipUnchnaged] $@"
        export obj="$obj $4"
    else
        echo "" "$@"
        "$@"
        local exit_status=$?
        if [ $exit_status -eq 0 ]; then
            echo $info >$ts
            export obj="$obj $4"
        else
            return "$exit_status"
        fi
    fi
}

export link_flags="-lcurl -lncurses"
export cc_flags="-O0 -DZ_SOLO -DNOLOGGER -I. -g -c" # 注意移除了链接库标志
export cpp_flags="--std=c++17 $cc_flags"                 # 同样移除了链接库标志
mkobj() {
    #### libutils
    call $clang_cpp utils/checksum.cpp -o $out/checksum.o $cpp_flags &&
        call $clang utils/path.cpp -o $out/path.o $cpp_flags &&
        call $clang_cpp utils/strlib.cpp -o $out/strlib.o $cpp_flags &&
        call $clang_cpp utils/file.cpp -o $out/file.o $cpp_flags &&
        call $clang_cpp utils/http.cpp -o $out/http.o $cpp_flags &&
        # ar rcs $out/libutils.a $out/checksum.o $out/path.o $out/strlib.o $out/file.o &&

        #### lib-datafile
        call $clang_cpp datafile/objectdata.cpp -o $out/objectdata.o $cpp_flags &&
        call $clang_cpp datafile/game.cpp -o $out/game.o $cpp_flags &&
        call $clang_cpp datafile/id.cpp -o $out/id.o $cpp_flags &&
        call $clang_cpp datafile/metadata.cpp -o $out/metadata.o $cpp_flags &&
        call $clang_cpp datafile/slk.cpp -o $out/slk.o $cpp_flags &&
        call $clang_cpp datafile/unitdata.cpp -o $out/unitdata.o $cpp_flags &&
        call $clang_cpp datafile/westrings.cpp -o $out/westrings.o $cpp_flags &&
        call $clang_cpp datafile/wtsdata.cpp -o $out/wtsdata.o $cpp_flags &&

        ## lib-rmpq
        call $clang_cpp rmpq/adpcm/adpcm.cpp -o $out/adpcm.o $cpp_flags &&
        call $clang_cpp rmpq/archive.cpp -o $out/archive.o $cpp_flags &&
        call $clang_cpp rmpq/common.cpp -o $out/common.o $cpp_flags &&
        call $clang_cpp rmpq/compress.cpp -o $out/compress.o $cpp_flags &&
        call $clang_cpp rmpq/huffman/huff.cpp -o $out/huff.o $cpp_flags &&
        call $clang_cpp rmpq/locale.cpp -o $out/locale.o $cpp_flags &&
        call $clang rmpq/pklib/crc32.c -o $out/crc32.o $cc_flags &&
        call $clang rmpq/pklib/explode.c -o $out/explode.o $cc_flags &&
        call $clang rmpq/pklib/implode.c -o $out/implode.o $cc_flags &&

        ## libimage
        call $clang_cpp image/image.cpp -o $out/image.o $cpp_flags &&
        call $clang_cpp image/imageblp.cpp -o $out/imageblp.o $cpp_flags &&
        call $clang_cpp image/imageblp2.cpp -o $out/imageblp2.o $cpp_flags &&
        call $clang_cpp image/imagedds.cpp -o $out/imagedds.o $cpp_flags &&
        call $clang_cpp image/imagegif.cpp -o $out/imagegif.o $cpp_flags &&
        call $clang_cpp image/imagejpg.cpp -o $out/imagejpg.o $cpp_flags &&
        call $clang_cpp image/imagepng.cpp -o $out/imagepng.o $cpp_flags &&
        call $clang_cpp image/imagetga.cpp -o $out/imagetga.o $cpp_flags &&

        #### libjpeg
        call $clang jpeg/source/jcapimin.c -o $out/jcapimin.o $cc_flags &&
        call $clang jpeg/source/jcapistd.c -o $out/jcapistd.o $cc_flags &&
        call $clang jpeg/source/jccoefct.c -o $out/jccoefct.o $cc_flags &&
        call $clang jpeg/source/jccolor.c -o $out/jccolor.o $cc_flags &&
        call $clang jpeg/source/jcdctmgr.c -o $out/jcdctmgr.o $cc_flags &&
        call $clang jpeg/source/jchuff.c -o $out/jchuff.o $cc_flags &&
        call $clang jpeg/source/jcinit.c -o $out/jcinit.o $cc_flags &&
        call $clang jpeg/source/jcmainct.c -o $out/jcmainct.o $cc_flags &&
        call $clang jpeg/source/jcmarker.c -o $out/jcmarker.o $cc_flags &&
        call $clang jpeg/source/jcmaster.c -o $out/jcmaster.o $cc_flags &&
        call $clang jpeg/source/jcomapi.c -o $out/jcomapi.o $cc_flags &&
        call $clang jpeg/source/jcparam.c -o $out/jcparam.o $cc_flags &&
        call $clang jpeg/source/jcphuff.c -o $out/jcphuff.o $cc_flags &&
        call $clang jpeg/source/jcprepct.c -o $out/jcprepct.o $cc_flags &&
        call $clang jpeg/source/jcsample.c -o $out/jcsample.o $cc_flags &&
        call $clang jpeg/source/jctrans.c -o $out/jctrans.o $cc_flags &&
        call $clang jpeg/source/jdapimin.c -o $out/jdapimin.o $cc_flags &&
        call $clang jpeg/source/jdapistd.c -o $out/jdapistd.o $cc_flags &&
        call $clang jpeg/source/jdatadst.c -o $out/jdatadst.o $cc_flags &&
        call $clang jpeg/source/jdatasrc.c -o $out/jdatasrc.o $cc_flags &&
        call $clang jpeg/source/jdcoefct.c -o $out/jdcoefct.o $cc_flags &&
        call $clang jpeg/source/jdcolor.c -o $out/jdcolor.o $cc_flags &&
        call $clang jpeg/source/jddctmgr.c -o $out/jddctmgr.o $cc_flags &&
        call $clang jpeg/source/jdhuff.c -o $out/jdhuff.o $cc_flags &&
        call $clang jpeg/source/jdinput.c -o $out/jdinput.o $cc_flags &&
        call $clang jpeg/source/jdmainct.c -o $out/jdmainct.o $cc_flags &&
        call $clang jpeg/source/jdmarker.c -o $out/jdmarker.o $cc_flags &&
        call $clang jpeg/source/jdmaster.c -o $out/jdmaster.o $cc_flags &&
        call $clang jpeg/source/jdmerge.c -o $out/jdmerge.o $cc_flags &&
        call $clang jpeg/source/jdphuff.c -o $out/jdphuff.o $cc_flags &&
        call $clang jpeg/source/jdpostct.c -o $out/jdpostct.o $cc_flags &&
        call $clang jpeg/source/jdsample.c -o $out/jdsample.o $cc_flags &&
        call $clang jpeg/source/jdtrans.c -o $out/jdtrans.o $cc_flags &&
        call $clang jpeg/source/jerror.c -o $out/jerror.o $cc_flags &&
        call $clang jpeg/source/jfdctflt.c -o $out/jfdctflt.o $cc_flags &&
        call $clang jpeg/source/jfdctfst.c -o $out/jfdctfst.o $cc_flags &&
        call $clang jpeg/source/jfdctint.c -o $out/jfdctint.o $cc_flags &&
        call $clang jpeg/source/jidctflt.c -o $out/jidctflt.o $cc_flags &&
        call $clang jpeg/source/jidctfst.c -o $out/jidctfst.o $cc_flags &&
        call $clang jpeg/source/jidctint.c -o $out/jidctint.o $cc_flags &&
        call $clang jpeg/source/jidctred.c -o $out/jidctred.o $cc_flags &&
        call $clang jpeg/source/jmemmgr.c -o $out/jmemmgr.o $cc_flags &&
        call $clang jpeg/source/jmemnobs.c -o $out/jmemnobs.o $cc_flags &&
        call $clang jpeg/source/jquant1.c -o $out/jquant1.o $cc_flags &&
        call $clang jpeg/source/jquant2.c -o $out/jquant2.o $cc_flags &&
        call $clang jpeg/source/jutils.c -o $out/jutils.o $cc_flags &&

        ### libzlib
        call $clang zlib/source/adler32.c -o $out/adler32.o $cc_flags &&
        call $clang zlib/source/compress.c -o $out/compress1.o $cc_flags &&
        call $clang zlib/source/crc32.c -o $out/crc321.o $cc_flags &&
        call $clang zlib/source/deflate.c -o $out/deflate.o $cc_flags &&
        call $clang zlib/source/infback.c -o $out/infback.o $cc_flags &&
        call $clang zlib/source/inffast.c -o $out/inffast.o $cc_flags &&
        call $clang zlib/source/inflate.c -o $out/inflate.o $cc_flags &&
        call $clang zlib/source/inftrees.c -o $out/inftrees.o $cc_flags &&
        call $clang zlib/source/trees.c -o $out/trees.o $cc_flags &&
        call $clang zlib/source/uncompr.c -o $out/uncompr.o $cc_flags &&
        call $clang zlib/source/zutil.c -o $out/zutil.o $cc_flags &&
        call $clang_cpp utils/common.cpp -o $out/common1.o $cpp_flags &&
        call $clang_cpp utils/logger.cpp -o $out/logger.o $cpp_flags &&
        call $clang_cpp ngdp/ngdp.cpp -o $out/ngdp.o $cpp_flags &&
        call $clang_cpp ngdp/cdnloader.cpp -o $out/cdnloader.o $cpp_flags &&
        call $clang_cpp jass.cpp -o $out/jass.o $cpp_flags &&
        call $clang_cpp detect.cpp -o $out/detect.o $cpp_flags &&
        call $clang_cpp utils/json.cpp -o $out/json.o $cpp_flags &&
        call $clang_cpp utils/utf8.cpp -o $out/utf8.o $cpp_flags &&
        call $clang_cpp parse.cpp -o $out/parse.o $cpp_flags &&
        call $clang_cpp search.cpp -o $out/search.o $cpp_flags &&
        call $clang_cpp hash.cpp -o $out/hash.o $cpp_flags &&
        call $clang_cpp icons.cpp -o $out/icons.o $cpp_flags &&
        call $clang_cpp main.cpp -o $out/main.o $cpp_flags &&
        echo $obj >$out/obj.txt
}

## app
mkapp() {
    echo ""
    echo $clang_cpp $(<"$out/obj.txt") -o $out/main $link_flags
    echo ""
    echo "Linking program:" $out/main
    $clang_cpp $(<"$out/obj.txt") -o $out/main $link_flags
    echo ""
    echo "Starting program without passing arguments:"
    echo ""
    $out/main
}
gendata(){
    $out/main "/wp4/WarCraft III/" -b 1.27.1.7085
}
mkall(){
mkobj && mkapp
}
mkall