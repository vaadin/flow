import {ComponentReference} from "../../component-util";
import {ComponentMetadata} from "../metadata/model";



export class ComponentOverlayManager {

    currentActiveComponent: ComponentReference | null = null;
    currentActiveComponentMetaData: ComponentMetadata | null = null;

    componentPicked = async (component: ComponentReference, metaData: ComponentMetadata) => {
          await this.hideOverlay();
          this.currentActiveComponent = component;
          this.currentActiveComponentMetaData = metaData;
    };

    showOverlay = () => {
        if(!this.currentActiveComponent || !this.currentActiveComponentMetaData){
            return;
        }
        if(this.currentActiveComponentMetaData.openOverlay){
            this.currentActiveComponentMetaData.openOverlay(this.currentActiveComponent);
        }
    }
    hideOverlay = () => {
        if(!this.currentActiveComponent || !this.currentActiveComponentMetaData){
            return;
        }
        if(this.currentActiveComponentMetaData.hideOverlay){
            this.currentActiveComponentMetaData.hideOverlay(this.currentActiveComponent);
        }
    };
    reset = () => {
        this.currentActiveComponent = null;
        this.currentActiveComponentMetaData = null;
    }
}
export const componentOverlayManager = new ComponentOverlayManager();


