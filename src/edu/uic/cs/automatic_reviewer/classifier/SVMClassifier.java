package edu.uic.cs.automatic_reviewer.classifier;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import edu.uic.cs.automatic_reviewer.feature.FeatureExtractor;
import edu.uic.cs.automatic_reviewer.feature.FeatureExtractor.FeatureType;
import edu.uic.cs.automatic_reviewer.input.Paper;
import edu.uic.cs.automatic_reviewer.input.PaperCache;
import edu.uic.cs.automatic_reviewer.input.PaperPublishType;
import libsvm.svm;
import libsvm.svm_parameter;
import libsvm.svm_problem;

public class SVMClassifier {
	private svm_parameter param;
	private svm_problem prob;	
	private String error_msg;
	private int nr_fold;
	//	private svm_model model;
	//	private int cross_validation;
	
	private void setDefaultParameters(){
		param = new svm_parameter();
		// default values
		param.svm_type = svm_parameter.C_SVC;
		param.kernel_type = svm_parameter.RBF;
		param.degree = 3;
		param.gamma = 0;	// 1/num_features
		param.coef0 = 0;
		param.nu = 0.5;
		param.cache_size = 100;
		param.C = 1;
		param.eps = 1e-3;
		param.p = 0.1;
		param.shrinking = 1;
		param.probability = 0;
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];		
		nr_fold = 10;
	}
	
	private void generateSVMProblem(Map<PaperPublishType, List<Paper>> paperMap){
		ArrayList<Paper> papers = new ArrayList<Paper>();
		int posNum = 0;
		
		List<Paper> posList = paperMap.get(PaperPublishType.LongPaper);
		if(posList != null){
			papers.addAll(posList);
			posNum = posList.size();
		}
		if(paperMap.containsKey(PaperPublishType.StudentWorkshopPaper)){
			papers.addAll(paperMap.get(PaperPublishType.StudentWorkshopPaper));
		}
		if(paperMap.containsKey(PaperPublishType.WorshopPaper)){
			papers.addAll(paperMap.get(PaperPublishType.WorshopPaper));
		}
		
		double y[] = new double[papers.size()];
		int i = 0;
		for(; i < posNum; ++i){
			y[i] = 1;
		}
		for(; i < y.length; ++i){
			y[i] = 0;
		}
		

		
		prob = new svm_problem();
		prob.l = y.length;
		prob.y = y;
		
		List<FeatureType> types = Arrays.asList(FeatureType.NumOfFiguresByPage,
				FeatureType.NumOfFormulasByPage, FeatureType.NumOfTablesByPage,
				FeatureType.TFIDF);
		FeatureExtractor featureExtractor = new FeatureExtractor();
		prob.x = featureExtractor.generateFeatureVectors(papers,types);

	}
	
	private void run(Map<PaperPublishType, List<Paper>> paperMap){
		setDefaultParameters();
		generateSVMProblem(paperMap);
		error_msg = svm.svm_check_parameter(prob,param);
		if(error_msg != null)
		{
			System.err.print("ERROR: "+error_msg+"\n");
			System.exit(1);
		}
		do_cross_validation();
	}
	
	
	private void do_cross_validation()
	{
		int i;
		int total_correct = 0;
		double total_error = 0;
		double sumv = 0, sumy = 0, sumvv = 0, sumyy = 0, sumvy = 0;
		double[] target = new double[prob.l];

		svm.svm_cross_validation(prob,param,nr_fold,target);
		if(param.svm_type == svm_parameter.EPSILON_SVR ||
		   param.svm_type == svm_parameter.NU_SVR)
		{
			for(i=0;i<prob.l;i++)
			{
				double y = prob.y[i];
				double v = target[i];
				total_error += (v-y)*(v-y);
				sumv += v;
				sumy += y;
				sumvv += v*v;
				sumyy += y*y;
				sumvy += v*y;
			}
			System.out.print("Cross Validation Mean squared error = "+total_error/prob.l+"\n");
			System.out.print("Cross Validation Squared correlation coefficient = "+
				((prob.l*sumvy-sumv*sumy)*(prob.l*sumvy-sumv*sumy))/
				((prob.l*sumvv-sumv*sumv)*(prob.l*sumyy-sumy*sumy))+"\n"
				);
		}
		else
		{
			for(i=0;i<prob.l;i++)
				if(target[i] == prob.y[i])
					++total_correct;
			System.out.print("Cross Validation Accuracy = "+100.0*total_correct/prob.l+"%\n");
		}
	}

	public static void main(String[] args) {
		Map<PaperPublishType, List<Paper>> result = PaperCache.getInstance()
		.getPapersByPublishTypeForYear(2012);
		System.out.println(2012);

		SVMClassifier classfier = new SVMClassifier();
		classfier.run(result);
	}
}