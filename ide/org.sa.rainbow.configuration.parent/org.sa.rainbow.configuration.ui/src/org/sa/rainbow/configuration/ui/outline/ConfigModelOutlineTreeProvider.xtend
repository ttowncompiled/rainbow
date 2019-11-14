/*
 * generated by Xtext 2.19.0
 */
package org.sa.rainbow.configuration.ui.outline

import org.eclipse.emf.ecore.EObject
import org.eclipse.swt.graphics.Image
import org.eclipse.xtext.ui.editor.outline.IOutlineNode
import org.eclipse.xtext.ui.editor.outline.impl.DefaultOutlineTreeProvider
import org.sa.rainbow.configuration.configModel.Assignment
import org.sa.rainbow.configuration.configModel.CommandCall
import org.sa.rainbow.configuration.configModel.CommandReference
import org.sa.rainbow.configuration.configModel.Component
import org.sa.rainbow.configuration.configModel.ConfigModelPackage
import org.sa.rainbow.configuration.configModel.Effector
import org.sa.rainbow.configuration.configModel.GaugeBody
import org.sa.rainbow.configuration.configModel.GaugeTypeBody
import org.sa.rainbow.configuration.configModel.Probe
import org.eclipse.emf.ecore.EStructuralFeature
import org.sa.rainbow.configuration.configModel.EffectorBody

/**
 * Customization of the default outline structure.
 * 
 * See https://www.eclipse.org/Xtext/documentation/310_eclipse_support.html#outline
 */
class ConfigModelOutlineTreeProvider extends DefaultOutlineTreeProvider {

	def Image nullImage() {
		return null;
	}

	def _createChildren(IOutlineNode parentNode, Component setup) {
		for (Assignment a : setup.assignment) {
			createNode(parentNode, a);
		}
	}

	def _createChildren(IOutlineNode parentNode, Assignment ass) {
		if (ass.value !== null && ass.value.value !== null && ass.value.value instanceof Component) {
			for (Assignment a : (ass.value.value as Component).assignment) {
				createNode(parentNode, a)
			}
		} else
			super._createChildren(parentNode, ass);
	}

	def _createNode(IOutlineNode parentNode, GaugeTypeBody gtb) {

		var model = ""
		if (gtb.mcf != null ) {
			model = '''...«gtb.mcf.simpleName»'''
		}

		createEStructuralFeatureNode(parentNode, gtb.eContainer, ConfigModelPackage.Literals.GAUGE_TYPE__NAME,
			nullImage(), model, true)
		for (cmd : gtb.commands) {
			super._createNode(parentNode, cmd)
		}
		for (ass : gtb.assignment) {
			super._createNode(parentNode, ass)
		}
	}

	def _CreateNode(IOutlineNode parentNode, GaugeBody gtb) {
		var model = ""
		var EStructuralFeature ref = ConfigModelPackage.Literals.GAUGE_TYPE__NAME
		if (gtb.modelName != null || gtb.modeltype != null) {
			model = '''«gtb.modelName===null?"":gtb.modelName»::«gtb.modeltype!=null?"":gtb.modeltype»'''
			ref = ConfigModelPackage.Literals.GAUGE_BODY__MODEL_NAME
		} else if (gtb.ref?.referable != null) {
			model = '''«gtb.ref.referable.name»'''
			ref = ConfigModelPackage.Literals.GAUGE_BODY__REF
		}
		createEStructuralFeatureNode(parentNode, gtb.eContainer, ref,
			nullImage(), model, true)
		for (cmd : gtb.commands) {
			super._createNode(parentNode, cmd)
		}
		for (ass : gtb.assignment) {
			super._createNode(parentNode, ass)
		}
	}

	def _createNode(IOutlineNode parentNode, Probe probe) {
		for (ass : probe?.properties?.assignment) {
			super._createNode(parentNode, ass)
		}
	}

	def _createNode(IOutlineNode parentNode, EffectorBody body) {
		var model = ""
		var EStructuralFeature ref = ConfigModelPackage.Literals.GAUGE_TYPE__NAME
		if (body.modelName != null || body.modeltype != null) {
			model = '''«body.modelName===null?"":body.modelName»::«body.modeltype!=null?"":body.modeltype»'''
			ref = ConfigModelPackage.Literals.EFFECTOR_BODY__MODEL_NAME
		} else if (body.ref?.referable != null) {
			model = '''«body.ref.referable.name»'''
			ref = ConfigModelPackage.Literals.EFFECTOR_BODY__REF
		}
		createEStructuralFeatureNode(parentNode, body, ref,
			nullImage(), model, true)
		for (ass : body.assignment) {
			super._createNode(parentNode, ass)
		}
	}

	def _createNode(IOutlineNode parentNode, CommandReference cr) {
		createEStructuralFeatureNode(parentNode, cr, ConfigModelPackage.Literals.COMMAND_REFERENCE__NAME, nullImage(),
			cr.name, true)
	}

	def _createNode(IOutlineNode parentNode, CommandCall cr) {
		createEStructuralFeatureNode(parentNode, cr, ConfigModelPackage.Literals.COMMAND_CALL__NAME, nullImage(),
			cr.name, true)
	}

	def _createNode(IOutlineNode parentNode, Assignment ass) {
		if (ass.value !== null && ass.value.value !== null && ass.value.value instanceof Component) {
			super._createNode(parentNode, ass);

		} else {
			createEStructuralFeatureNode(parentNode, ass, ConfigModelPackage.Literals.ASSIGNMENT__NAME, nullImage(),
				ass.name, true);
		}
	}

//	def _createChildren(IOutlineNode parentNode, Value v) {
//		var value = v.value;
//		if (value instanceof Component) {
//			var comp = value as Component;
//			for (Assignment a : comp.assignment) {
//				createNode(parentNode, a)
//			}
//		}
//		else {
//			super._createChildren(parentNode,v);
//		}
//		
//	}
}
