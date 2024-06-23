package com.example.merging_2024;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;


import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.awt.image.BufferedImage;
import java.util.List;
import java.io.IOException;
import java.util.Base64;

@RestController
@RequestMapping("/image")
@SpringBootApplication
public class MergingApplication {

	public MatrixSolver solver;
	public Thread blendingThread;
	public int state;

	public static final int NOTHING = 0;
	public static final int SELECTING = 1;
	public static final int DRAGGING = 2;
	public static final int BLENDING = 3;


/*	public void nextIteration() {
		for (int i = 0; i < 100; i++) {
			solver.nextIteration();
		}
		synchronized (selectedImage) {
			solver.updateImage(selectedImage);
		}

	}



	public void finalizeBlending() {


		Graphics g = image.getGraphics();

		// place the selectedImage that have benn changed it edges color, place it on top of image(the whole screen)
		g.drawImage(selectedImage, xMin, yMin, null);

		// ImageSaver.saveImage(image, imagePath, "jpg", 0, 0, sourceImage.getWidth(), sourceImage.getHeight());

		selectedImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		selectionBorder.clear();
		selectionArea.clear();
		state = NOTHING;
	}*/






/*	class IterationBlender implements Runnable {

		public void run() {
			int iteration = 0;
			double error;
			double Norm = 1.0;

			do {
				error = solver.getError();

				if (iteration == 1) {
					Norm = Math.log(error);
				}
				if (iteration >= 1) {
					//The Jacobi method converges exponentially
					double progress = 1.0 - Math.log(error) / Norm;

				}
				iteration++;
				nextIteration();
			} while (error > 1.0 && state == BLENDING);
			finalizeBlending();
			System.out.println("Did " + iteration + "iterations");

		}
	}*/



	@PostMapping
	public ResponseEntity<?> uploadImage(
			@RequestBody UploadRequest uploadRequest
	) throws IOException {

			String base64TargetImage = uploadRequest.getTargetImage();
			String base64CroppedImage = uploadRequest.getCroppedImage();

			ArrayList<Cord> selectionArea = uploadRequest.getCords();
			int[][] mask = uploadRequest.getMask();

		// the base64 string into bytes
	//	byte[] targetImageData = Base64.getDecoder().decode(base64TargetImage);
	//	byte[] croppedImageData = Base64.getDecoder().decode(base64CroppedImage);

		BufferedImage bufferedTargetImage =	Base64ToBufferedImage.base64StringToImage(base64TargetImage);
		BufferedImage bufferedCroppedImage =	Base64ToBufferedImage.base64StringToImage(base64CroppedImage);


		System.out.println("Cropped image height == "+ bufferedCroppedImage.getHeight());
		System.out.println("Cropped image width == "+ bufferedCroppedImage.getWidth());

		String bufferedTargetImagePath = "c:\\testSavedImages\\bufferedTargetImage.jpg";
		String bufferedCroppedImagePath = "c:\\testSavedImages\\bufferedCroppedImage.jpg";

		ImageSaver.saveImage(bufferedCroppedImage, bufferedCroppedImagePath, "jpg" );
		ImageSaver.saveImage(bufferedTargetImage, bufferedTargetImagePath, "jpg" );

		

/*
		solver = new MatrixSolver(mask, selectionArea, bufferedTargetImage, bufferedCroppedImage, 0, 0);
		PoissonStandalone.IterationBlender blender = new PoissonStandalone.IterationBlender();
		blendingThread = new Thread(blender);
		blendingThread.start();*/




		return ResponseEntity.status(HttpStatus.OK).body("server done it things...");
	}



	public static void main(String[] args) {
		SpringApplication.run(MergingApplication.class, args);

		System.out.println("Hello Im in main...............");




	}

}
