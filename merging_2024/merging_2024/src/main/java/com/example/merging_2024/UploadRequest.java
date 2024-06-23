package com.example.merging_2024;


import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class UploadRequest {

    private String targetImage;
    private String croppedImage;
    private
    ArrayList<Cord> cords;
    private int[][] mask;
}