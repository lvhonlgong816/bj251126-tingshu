package com.atguigu.tingshu;

import com.atguigu.tingshu.album.AlbumFeignClient;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ServiceSearchApplicationTest {

    @Autowired
    private AlbumFeignClient albumFeignClient;


    @Test
    public void testFeign(){
        Result<AlbumInfo> result = albumFeignClient.getAlbumInfo(1L);
        System.out.println(result);
    }

}
